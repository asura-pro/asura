package asura.core.scenario.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor._
import asura.common.util.LogUtils
import asura.core.ErrorMessages
import asura.core.es.model.JobReportData.ScenarioReportItemData
import asura.core.es.model.{HttpCaseRequest, ScenarioStep}
import asura.core.es.service.HttpCaseRequestService
import asura.core.runtime.ContextOptions
import asura.core.scenario.ScenarioRunner
import asura.core.scenario.actor.ScenarioTestActor.ScenarioTestMessage

import scala.collection.mutable.ArrayBuffer

class ScenarioTestActor(user: String, out: ActorRef) extends BaseActor {

  implicit val executionContext = context.dispatcher
  if (null != out) self ! SenderMessage(out)

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
  }

  def handleRequest(wsActor: ActorRef): Receive = {
    case ScenarioTestMessage(summary, steps, options) =>
      val caseIds = steps.filter(ScenarioStep.TYPE_HTTP == _.`type`).map(_.id)
      if (null != steps && steps.nonEmpty) {
        HttpCaseRequestService.getByIdsAsMap(caseIds).map(caseIdMap => {
          val cases = ArrayBuffer[(String, HttpCaseRequest)]()
          caseIds.foreach(id => {
            val value = caseIdMap.get(id)
            if (value.nonEmpty) {
              cases.append((id, value.get))
            }
          })
          ScenarioRunner.test("ScenarioTestActor", summary, cases, logMsg => {
            wsActor ! NotifyActorEvent(logMsg)
          }, options, logEvent => {
            wsActor ! logEvent
          }).pipeTo(self)
        })
      } else {
        wsActor ! ErrorActorEvent(ErrorMessages.error_EmptyCase.errMsg)
        wsActor ! PoisonPill
      }
    case report: ScenarioReportItemData =>
      wsActor ! OverActorEvent(report)
      wsActor ! PoisonPill
    case eventMessage: ActorEvent =>
      wsActor ! eventMessage
    case Status.Failure(t) =>
      val logErrMsg = LogUtils.stackTraceToString(t)
      log.warning(logErrMsg)
      wsActor ! ErrorActorEvent(t.getMessage)
      wsActor ! PoisonPill
    case _ =>
      wsActor ! ErrorActorEvent(ErrorMessages.error_UnknownMessageType.errMsg)
      wsActor ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioTestActor {

  def props(user: String, out: ActorRef = null) = Props(new ScenarioTestActor(user, out))

  case class ScenarioTestMessage(summary: String, steps: Seq[ScenarioStep], options: ContextOptions)

}
