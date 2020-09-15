package asura.core.es.service

import java.nio.charset.StandardCharsets
import java.util.Base64

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, RSAUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsResponse}
import asura.core.model.QuerySqlRequest
import asura.core.sql.SqlParserUtils
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import asura.core.{CoreConfig, ErrorMessages}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Iterable, mutable}
import scala.concurrent.Future

object SqlRequestService extends CommonService with BaseAggregationService {

  val basicFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_LABELS,
    FieldKeys.FIELD_OBJECT_REQUEST_HOST,
    FieldKeys.FIELD_OBJECT_REQUEST_PORT,
    FieldKeys.FIELD_OBJECT_REQUEST_DATABASE,
    FieldKeys.FIELD_OBJECT_REQUEST_TABLE
  )
  val queryFields = basicFields ++ Seq(
    FieldKeys.FIELD_EXPORTS,
  )

  def index(doc: SqlRequest): Future[IndexDocResponse] = {
    val error = validate(doc)
    if (null == error) {
      EsClient.esClient.execute {
        indexInto(SqlRequest.Index).doc(doc).refresh(RefreshPolicy.WAIT_FOR)
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
        delete(id).from(SqlRequest.Index).refresh(RefreshPolicy.WAIT_FOR)
      }.map(toDeleteDocResponse(_))
    }
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(ids.map(id => delete(id).from(SqlRequest.Index)))
      }.map(toBulkDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(SqlRequest.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def getRequestById(id: String): Future[SqlRequest] = {
    EsClient.esClient.execute {
      search(SqlRequest.Index).query(idsQuery(id)).size(1)
    }.map(res => {
      if (res.isSuccess && res.result.nonEmpty) {
        JacksonSupport.parse(res.result.hits.hits(0).sourceAsString, classOf[SqlRequest])
      } else {
        null
      }
    })
  }

  private def getByIds(ids: Seq[String], filterFields: Boolean = false) = {
    if (null != ids) {
      EsClient.esClient.execute {
        search(SqlRequest.Index)
          .query(idsQuery(ids))
          .from(0)
          .size(ids.length)
          .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
          .sourceInclude(if (filterFields) queryFields else Nil)
      }
    } else {
      ErrorMessages.error_EmptyId.toFutureFail
    }
  }

  def getByIdsAsMap(ids: Seq[String], filterFields: Boolean = false): Future[Map[String, SqlRequest]] = {
    if (null != ids && ids.nonEmpty) {
      val map = mutable.HashMap[String, SqlRequest]()
      getByIds(ids, filterFields).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdsNotFound(ids).toException
          } else {
            res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[SqlRequest])))
            map.toMap
          }
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  def getByIdsAsRawMap(ids: Iterable[String]) = {
    if (null != ids && ids.nonEmpty) {
      EsClient.esClient.execute {
        search(SqlRequest.Index).query(idsQuery(ids)).size(ids.size).sourceInclude(basicFields)
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  def query(q: QuerySqlRequest, excludeInScn: Boolean = true): Future[Map[String, Any]] = {
    val esQueries = ArrayBuffer[Query]()
    if (excludeInScn) {
      esQueries += boolQuery().not(existsQuery(FieldKeys.FIELD_IN_SCN))
    }
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    if (StringUtils.isNotEmpty(q.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, q.group)
    if (StringUtils.isNotEmpty(q.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, q.project)
    if (StringUtils.isNotEmpty(q.host)) esQueries += termQuery(FieldKeys.FIELD_OBJECT_REQUEST_HOST, q.host)
    if (StringUtils.isNotEmpty(q.database)) esQueries += termQuery(FieldKeys.FIELD_OBJECT_REQUEST_DATABASE, q.database)
    if (StringUtils.isNotEmpty(q.table)) esQueries += termQuery(FieldKeys.FIELD_OBJECT_REQUEST_TABLE, q.table)
    if (StringUtils.isNotEmpty(q.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, q.text)
      sortFields = Nil
    }
    if (StringUtils.isNotEmpty(q.sql)) esQueries += matchQuery(FieldKeys.FIELD_SQL, q.sql)
    EsClient.esClient.execute {
      search(SqlRequest.Index).query(boolQuery().must(esQueries))
        .from(q.pageFrom)
        .size(q.pageSize)
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

  def updateDoc(id: String, doc: SqlRequest) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = validate(doc)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          val (src, params) = doc.toUpdateScriptParams
          update(id).in(SqlRequest.Index)
            .script {
              script(src).params(params)
            }
        }.map(toUpdateDocResponse(_))
      }
    }
  }

  def validate(doc: SqlRequest): ErrorMessage = {
    try {
      if (null == doc || null == doc.request) {
        ErrorMessages.error_EmptyRequestBody
      } else {
        val request = doc.request
        try {
          val table = SqlParserUtils.getStatementTable(request.sql)
          request.table = table
        } catch {
          case _: Throwable => request.table = StringUtils.EMPTY // the sql maybe need to be rendered
        }
        val securityConfig = CoreConfig.securityConfig
        if (StringUtils.isNotEmpty(request.password) && !request.password.equals(securityConfig.maskText)) {
          // encrypt password
          val encryptedBytes = RSAUtils.encryptByPrivateKey(
            request.password.getBytes(StandardCharsets.UTF_8), securityConfig.priKeyBytes)
          request.encryptedPass = new String(Base64.getEncoder.encode(encryptedBytes))
          request.password = securityConfig.maskText
        }
        if (StringUtils.isEmpty(doc.group)) {
          ErrorMessages.error_EmptyGroup
        } else if (StringUtils.isEmpty(doc.project)) {
          ErrorMessages.error_EmptyProject
        } else if (StringUtils.hasEmpty(request.host, request.username, request.password, request.encryptedPass, request.database, request.sql)) {
          ErrorMessages.error_InvalidRequestParameters
        } else {
          null
        }
      }
    } catch {
      case t: Throwable => ErrorMessages.error_Throwable(t)
    }
  }
}
