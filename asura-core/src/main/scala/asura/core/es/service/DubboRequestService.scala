package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryDubboRequest
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object DubboRequestService extends CommonService with BaseAggregationService {

  val queryFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_LABELS,
    FieldKeys.FIELD_INTERFACE,
    FieldKeys.FIELD_METHOD,
    FieldKeys.FIELD_PARAMETER_TYPES,
  )

  def index(doc: DubboRequest): Future[IndexDocResponse] = {
    val error = validate(doc)
    if (null == error) {
      EsClient.esClient.execute {
        indexInto(DubboRequest.Index / EsConfig.DefaultType).doc(doc).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    } else {
      error.toFutureFail
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(DubboRequest.Index).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(ids.map(id => delete(id).from(DubboRequest.Index)))
      }.map(toBulkDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(DubboRequest.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def query(q: QueryDubboRequest): Future[Map[String, Any]] = {
    val esQueries = ArrayBuffer[Query]()
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    if (StringUtils.isNotEmpty(q.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, q.group)
    if (StringUtils.isNotEmpty(q.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, q.project)
    if (StringUtils.isNotEmpty(q.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, q.text)
      sortFields = Nil
    }
    if (StringUtils.isNotEmpty(q.interface)) esQueries += wildcardQuery(FieldKeys.FIELD_INTERFACE, s"*${q.interface}*")
    if (StringUtils.isNotEmpty(q.method)) esQueries += termQuery(FieldKeys.FIELD_METHOD, q.method)
    EsClient.esClient.execute {
      search(DubboRequest.Index).query(boolQuery().must(esQueries))
        .from(q.pageFrom)
        .sortBy(sortFields)
        .sourceInclude(queryFields)
    }.flatMap(res => {
      fetchWithCreatorProfiles(res)
      if (res.isSuccess) {
        if (q.hasCreators) {
          fetchWithCreatorProfiles(res)
        } else {
          Future.successful(EsResponse.toApiData(res.result, true))
        }
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    })
  }

  def updateDoc(id: String, doc: DubboRequest) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = validate(doc)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          val (src, params) = doc.toUpdateScriptParams
          update(id).in(DubboRequest.Index / EsConfig.DefaultType)
            .script {
              script(src).params(params)
            }
        }.map(toUpdateDocResponse(_))
      }
    }
  }

  def validate(doc: DubboRequest): ErrorMessages.ErrorMessage = {
    if (null == doc) {
      ErrorMessages.error_EmptyRequestBody
    } else if (StringUtils.isEmpty(doc.group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(doc.project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.hasEmpty(doc.interface, doc.method, doc.address) ||
      null == doc.parameterTypes || null == doc.args) {
      ErrorMessages.error_InvalidRequestParameters
    } else {
      null
    }
  }
}
