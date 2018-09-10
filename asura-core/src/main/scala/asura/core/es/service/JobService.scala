package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.job.actor.JobStatusActor.JobQueryMessage
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobService extends CommonService {

  def index(job: Job): Future[IndexDocResponse] = {
    if (null == job) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val (isOK, errMsg) = validate(job)
      if (!isOK) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        val jobId = Job.buildJobKey(job)
        EsClient.esClient.execute {
          exists(jobId, Job.Index, EsConfig.DefaultType)
        }.flatMap(res => {
          if (res.isSuccess) {
            if (res.result) {
              FutureUtils.illegalArgs(s"${job.scheduler}:${job.group}:${job.name} already exists.")
            } else {
              EsClient.esClient.execute {
                indexInto(Job.Index / EsConfig.DefaultType).doc(job).id(jobId).refresh(RefreshPolicy.WAIT_UNTIL)
              }.map(toIndexDocResponse(_))
            }
          } else {
            FutureUtils.requestFail(res.error.reason)
          }
        })
      }
    }
  }


  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(Job.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
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

  def updateJob(job: Job): Future[UpdateDocResponse] = {
    if (null == job) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val (isOk, errMsg) = validate(job)
      if (!isOk) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.esClient.execute {
          update(Job.buildJobKey(job)).in(Job.Index / EsConfig.DefaultType).doc(JacksonSupport.stringify(job.toUpdateMap))
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

  def validate(job: Job): BoolErrorRes = {
    if (StringUtils.isEmpty(job.summary)) {
      (false, "Empty job name")
    } else if (StringUtils.isEmpty(job.classAlias)) {
      (false, "Empty job class")
    } else if (StringUtils.isEmpty(job.group)) {
      (false, "Empty job group")
    } else if (StringUtils.isEmpty(job.scheduler)) {
      (false, "Empty job scheduler")
    } else {
      (true, null)
    }
  }


  def geJobById(id: String): Future[Job] = {
    getById(id).map(res => {
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          throw IllegalRequestException(s"Api: ${id} not found.")
        } else {
          val hit = res.result.hits.hits(0)
          JacksonSupport.parse(hit.sourceAsString, classOf[Job])
        }
      } else
        throw RequestFailException(res.error.reason)
    })
  }

  def query(query: JobQueryMessage) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.scheduler)) {
      esQueries += termQuery(FieldKeys.FIELD_SCHEDULER, query.scheduler)
    }
    if (StringUtils.isNotEmpty(query.group)) {
      esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    }
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    }
    EsClient.esClient.execute {
      if (Option(query.from).isEmpty || query.from < 0) query.from = 0
      if (Option(query.size).isEmpty || query.size < 0) query.size = 0
      val clause = search(Job.Index).query {
        boolQuery().must(esQueries)
      }.from(query.from).size(query.size)
      clause
    }
  }
}
