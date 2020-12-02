package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.exceptions.RequestFailException
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.model.QueryJob
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.{NestedQuery, Query}
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.Iterable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobService extends CommonService with BaseAggregationService {

  val basicFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
  )
  val queryIncludeFields = basicFields ++ Seq(
    FieldKeys.FIELD_TRIGGER,
  )

  def index(job: Job): Future[IndexDocResponse] = {
    if (null == job) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val error = validate(job)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          indexInto(Job.Index / EsConfig.DefaultType).doc(job).refresh(RefreshPolicy.WaitFor)
        }.map(toIndexDocResponse(_))
      }
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(Job.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WaitFor)
      }.map(toDeleteDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(Job.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def getById(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(Job.Index).query(idsQuery(ids)).from(0).size(ids.length)
      }
    }
  }

  def updateJob(id: String, job: Job): Future[UpdateDocResponse] = {
    if (null == job || StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val error = validate(job)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          val (src, params) = job.toUpdateScriptParams
          update(id).in(Job.Index / EsConfig.DefaultType).script {
            script(src).params(params)
          }
        }.map(toUpdateDocResponse(_))
      }
    }
  }

  def docCount(path: String, project: String) = {
    EsClient.esClient.execute {
      count(Job.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_PATH, path),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }
    }
  }

  def docCount(path: String, method: String, version: String, project: String) = {
    EsClient.esClient.execute {
      count(Job.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_PATH, path),
          termQuery(FieldKeys.FIELD_METHOD, method),
          termQuery(FieldKeys.FIELD_VERSION, version),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }
    }
  }

  def validate(job: Job): ErrorMessage = {
    if (StringUtils.isEmpty(job.summary)) {
      ErrorMessages.error_EmptyJobName
    } else if (StringUtils.isEmpty(job.classAlias)) {
      ErrorMessages.error_EmptyJobType
    } else if (StringUtils.isEmpty(job.group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(job.scheduler)) {
      ErrorMessages.error_EmptyScheduler
    } else {
      null
    }
  }

  def getJobById(id: String): Future[Job] = {
    getById(id).map(res => {
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          throw ErrorMessages.error_IdNonExists.toException
        } else {
          val hit = res.result.hits.hits(0)
          JacksonSupport.parse(hit.sourceAsString, classOf[Job])
        }
      } else
        throw RequestFailException(res.error.reason)
    })
  }

  def queryJob(query: QueryJob) = {
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.triggerType)) esQueries += nestedQuery(FieldKeys.FIELD_TRIGGER, termQuery(FieldKeys.FIELD_NESTED_TRIGGER_TRIGGER_TYPE, query.triggerType))
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      sortFields = Nil
    }
    EsClient.esClient.execute {
      search(Job.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(sortFields)
        .sourceInclude(queryIncludeFields)
    }
  }

  def containEnv(ids: Seq[String]) = {
    val query = boolQuery().must(termsQuery(FieldKeys.FIELD_ENV, ids))
    EsClient.esClient.execute {
      search(Job.Index).query(query)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields)
    }
  }

  def containCase(caseIds: Seq[String]) = {
    val query = NestedQuery(FieldKeys.FIELD_NESTED_JOB_DATA_CS,
      boolQuery().must(termsQuery(FieldKeys.FIELD_NESTED_JOB_DATA_CS_ID, caseIds))
    )
    EsClient.esClient.execute {
      search(Job.Index).query(query)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields)
    }
  }

  def containScenario(ids: Seq[String]) = {
    val query = NestedQuery(FieldKeys.FIELD_NESTED_JOB_DATA_SCENARIO,
      boolQuery().must(termsQuery(FieldKeys.FIELD_NESTED_JOB_DATA_SCENARIO_ID, ids))
    )
    EsClient.esClient.execute {
      search(Job.Index).query(query)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields)
    }
  }

  def getByIdsAsRawMap(ids: Iterable[String]) = {
    if (null != ids && ids.nonEmpty) {
      EsClient.esClient.execute {
        search(Job.Index).query(idsQuery(ids)).size(ids.size).sourceInclude(basicFields)
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  def getRelativesById(id: String): Future[Map[String, Any]] = {
    getJobById(id).flatMap(job => {
      val scenarioIds = if (null != job.jobData.scenario) {
        job.jobData.scenario.filter(_.isScenarioStep()).map(_.id)
      } else {
        Nil
      }
      ScenarioService.getByIdsAsRawMap(scenarioIds).map(m => {
        Map("job" -> job, "scenarios" -> m, "jobId" -> id)
      })
    })
  }
}
