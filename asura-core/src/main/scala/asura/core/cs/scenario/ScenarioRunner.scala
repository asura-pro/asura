package asura.core.cs.scenario

import asura.common.actor.{ActorEvent, ItemActorEvent}
import asura.common.util.{LogUtils, StringUtils, XtermUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.assertion.engine.Statistic
import asura.core.cs.{CaseContext, CaseResult, CaseRunner, ContextOptions}
import asura.core.es.model.JobReportData.{CaseReportItem, ReportItemStatus, ScenarioReportItem}
import asura.core.es.model.{Case, Scenario, ScenarioStep}
import asura.core.es.service.{CaseService, ScenarioService}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ScenarioRunner {

  case class ReportItemEvent(status: String, errMsg: String, result: CaseResult)

  val logger = Logger("ScenarioRunner")

  def testScenarios(
                     scenarioIds: Seq[String],
                     log: String => Unit = null,
                     options: ContextOptions = null
                   ): Future[Seq[ScenarioReportItem]] = {
    val scenarioIdMap = scala.collection.mutable.HashMap[String, Scenario]()
    val scenarioIdCaseIdMap = scala.collection.mutable.HashMap[String, Seq[String]]()
    if (null != scenarioIds && scenarioIds.nonEmpty) {
      ScenarioService.getScenariosByIds(scenarioIds).flatMap(list => {
        val caseIds = ArrayBuffer[String]()
        list.foreach(tuple => {
          val (scenarioId, scenario) = tuple
          val scenarioCaseIds = scenario.steps.filter(ScenarioStep.TYPE_CASE == _.`type`).map(_.id)
          scenarioIdMap += (scenarioId -> scenario)
          caseIds ++= scenarioCaseIds
          scenarioIdCaseIdMap(scenarioId) = scenarioCaseIds
        })
        CaseService.getCasesByIdsAsMap(caseIds)
      }).flatMap(caseIdMap => {
        val scenarioIdCaseMap = scala.collection.mutable.HashMap[String, Seq[(String, Case)]]()
        scenarioIdCaseIdMap.foreach(tuple => {
          val (scenarioId, caseIds) = tuple
          // if case was deleted the scenario will ignore it
          val cases = ArrayBuffer[(String, Case)]()
          caseIds.foreach(id => {
            val value = caseIdMap.get(id)
            if (value.nonEmpty) {
              cases.append((id, value.get))
            }
          })
          scenarioIdCaseMap(scenarioId) = cases
        })
        val jobReportItemsFutures = scenarioIdCaseMap.map(tuple => {
          val (scenarioId, cases) = tuple
          val scenario = scenarioIdMap(scenarioId)
          test(scenarioId, scenario.summary, cases, log, options)
        })
        Future.sequence(jobReportItemsFutures.toSeq)
      })
    } else {
      Future.successful(Nil)
    }
  }

  /**
    * @param scenarioId if this value is null, previous case context should not be put context
    * @param caseTuples (caseId, case)
    */
  def test(
            scenarioId: String,
            summary: String,
            caseTuples: Seq[(String, Case)],
            log: String => Unit = null,
            options: ContextOptions = null,
            logResult: ActorEvent => Unit = null,
          ): Future[ScenarioReportItem] = {
    if (null != log) log(s"scenario(${summary}): fetch ${caseTuples.length} cases.")
    val scenarioReportItem = ScenarioReportItem(scenarioId, summary)
    val caseReportItems = ArrayBuffer[CaseReportItem]()
    // for `foldLeft` type inference
    val nullCaseReportItem: CaseReportItem = null
    // it will be true only in a real scenario
    val failFast = StringUtils.isNotEmpty(scenarioId)
    var isScenarioFailed = false
    val caseContext = CaseContext(options = options)
    caseTuples.foldLeft(Future.successful(nullCaseReportItem))((prevCaseReportItemFuture, tuple) => {
      val (id, cs) = tuple
      for {
        prevReportItem <- prevCaseReportItemFuture
        currReportItem <- {
          if (null != prevReportItem) { // not the initial value of `foldLeft`
            caseReportItems += prevReportItem
          }
          ///////////////////////////////
          // generate case report item //
          ///////////////////////////////
          if (failFast && isScenarioFailed) {
            // add skipped test case report item in a scenario
            val item = CaseReportItem(id, cs.summary, Statistic())
            item.status = ReportItemStatus.STATUS_SKIPPED
            item.msg = ReportItemStatus.STATUS_SKIPPED
            if (null != log) log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.yellowWrap(ReportItemStatus.STATUS_SKIPPED)}.")
            if (null != logResult) logResult(ItemActorEvent(ReportItemEvent(item.status, null, null)))
            Future.successful(item)
          } else {
            // execute next test case
            CaseRunner.test(id, cs, caseContext)
              .map { caseResult =>
                val statis = caseResult.statis
                val item = if (statis.isSuccessful) {
                  if (null != log) log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.greenWrap(ReportItemStatus.STATUS_PASS)}.")
                  if (StringUtils.isNotEmpty(scenarioId)) {
                    // when it's a real scenario instead of a plain array of case
                    caseContext.setPrevCurrentData(CaseContext.extractCaseSelfContext(caseResult))
                  }
                  CaseReportItem.parse(cs.summary, caseResult)
                } else {
                  if (null != log) log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.redWrap(ReportItemStatus.STATUS_FAIL)}.")
                  isScenarioFailed = true
                  scenarioReportItem.markFail()
                  // fail because of assertions not pass
                  CaseReportItem.parse(cs.summary, caseResult)
                }
                if (null != logResult) logResult(ItemActorEvent(ReportItemEvent(item.status, null, caseResult)))
                item
              }
              .recover {
                case t: Throwable => {
                  // fail because of an exception was thrown
                  val errorStack = LogUtils.stackTraceToString(t)
                  logger.warn(errorStack)
                  if (null != log) {
                    log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.redWrap(ReportItemStatus.STATUS_FAIL)}.")
                    log(s"scenario(${summary}): ${cs.summary} error : ${errorStack}.")
                  }
                  val item = CaseReportItem.parse(cs.summary, CaseResult.failResult(id), msg = errorStack)
                  if (null != logResult) logResult(ItemActorEvent(ReportItemEvent(item.status, errorStack, null)))
                  item
                }
              }
          }
        }
      } yield currReportItem
    }).map(lastReportItem => {
      // last report item
      caseReportItems += lastReportItem
      scenarioReportItem.cases = caseReportItems
      if (StringUtils.isNotEmpty(scenarioId)) {
        // not in real scenario
        if (null != log) log(s"scenario(${summary}): ${
          if (scenarioReportItem.isSuccessful())
            XtermUtils.greenWrap(scenarioReportItem.msg)
          else
            XtermUtils.redWrap(scenarioReportItem.msg)
        }")
      }
      scenarioReportItem
    })
  }
}
