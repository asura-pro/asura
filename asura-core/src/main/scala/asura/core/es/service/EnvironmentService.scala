package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.auth.AuthManager
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.model.QueryEnv
import asura.core.es.model.{DeleteDocResponse, Environment, FieldKeys, IndexDocResponse}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

object EnvironmentService extends CommonService {

  val logger = Logger("EnvironmentService")

  def index(env: Environment): Future[IndexDocResponse] = {
    val error = validate(env)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.esClient.execute {
        indexInto(Environment.Index / EsConfig.DefaultType).doc(env).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        delete(id).from(Environment.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        search(Environment.Index).query(idsQuery(id)).size(1).sourceExclude(defaultExcludeFields)
      }
    }
  }

  def updateEnv(id: String, env: Environment) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = validate(env)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          update(id).in(Environment.Index / EsConfig.DefaultType)
            .doc(JacksonSupport.stringify(env.toUpdateMap))
            .refresh(RefreshPolicy.WAIT_UNTIL)
        }.map(toUpdateDocResponse(_))
      }
    }
  }

  def validate(env: Environment): ErrorMessages.ErrorMessage = {
    if (StringUtils.isEmpty(env.summary)) {
      ErrorMessages.error_EmptySummary
    } else if (StringUtils.isEmpty(env.group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(env.project)) {
      ErrorMessages.error_EmptyProject
    } else {
      val custom = env.custom
      val error = if (null != custom && custom.nonEmpty) {
        if (custom.forall(kv => StringUtils.isNotEmpty(kv.key) && StringUtils.isNotEmpty(kv.value))) {
          null
        } else {
          ErrorMessages.error_NullKeyOrValue
        }
      } else {
        null
      }
      if (null == error) {
        validateEnvAuth(env)
      } else {
        error
      }
    }
  }

  private def validateEnvAuth(env: Environment): ErrorMessages.ErrorMessage = {
    if (null != env.auth && env.auth.nonEmpty) {
      val errMsgs = ArrayBuffer[String]()
      val ret = env.auth.forall(auth => {
        val validateOpt = AuthManager(auth.`type`)
        if (validateOpt.nonEmpty) {
          val (isOk, errMsg) = validateOpt.get.validate(auth)
          if (!isOk) {
            errMsgs += errMsg
          }
          isOk
        } else {
          errMsgs += s"no ${auth.`type`} registered."
          false
        }
      })
      if (!ret) {
        ErrorMessages.error_Msgs(errMsgs)
      } else {
        null
      }
    } else {
      null
    }
  }

  def getEnvById(id: String)(implicit executor: ExecutionContext): Future[Environment] = {
    if (StringUtils.isEmpty(id)) {
      Future.successful(null)
    } else {
      getById(id).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdNonExists.toException
          } else {
            val hit = res.result.hits.hits(0)
            JacksonSupport.parse(hit.sourceAsString, classOf[Environment])
          }
        } else {
          throw RequestFailException(res.error.reason)
        }
      })
    }
  }

  def queryEnv(query: QueryEnv) = {
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      sortFields = Nil
    }
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    EsClient.esClient.execute {
      search(Environment.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(sortFields)
        .sourceInclude(defaultIncludeFields)
    }
  }
}
