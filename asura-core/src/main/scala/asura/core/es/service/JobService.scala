package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryJob
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobService extends CommonService {

  val queryIncludeFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
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
          indexInto(Job.Index / EsConfig.DefaultType).doc(job).refresh(RefreshPolicy.WAIT_UNTIL)
        }.map(toIndexDocResponse(_))
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

  def updateJob(id: String, job: Job): Future[UpdateDocResponse] = {
    if (null == job || StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val error = validate(job)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          update(id).in(Job.Index / EsConfig.DefaultType).doc(JacksonSupport.stringify(job.toUpdateMap))
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

  def validate(job: Job): ErrorMessages.Val = {
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

  def queryJob(query: QueryJob) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    EsClient.esClient.execute {
      search(Job.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(queryIncludeFields)
    }
  }
}
