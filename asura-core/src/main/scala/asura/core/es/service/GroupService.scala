package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.model.QueryGroup
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import asura.core.util.{CommonValidator, JacksonSupport}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.typesafe.scalalogging.Logger

import scala.collection.Iterable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object GroupService extends CommonService {

  val logger = Logger("GroupService")

  val groupRelatedIndexes = Seq(HttpCaseRequest.Index, Job.Index, Environment.Index,
    JobReport.Index, JobNotify.Index, Project.Index, Scenario.Index, Activity.Index,
    ProjectApiCoverage.Index, DubboRequest.Index, SqlRequest.Index, CiTrigger.Index, Favorite.Index,
    Permissions.Index
  )

  def index(group: Group, checkExists: Boolean = true): Future[IndexDocResponse] = {
    if (!CommonValidator.isIdLegal(group.id)) {
      ErrorMessages.error_IllegalGroupId.toFutureFail
    } else {
      if (checkExists) {
        docExists(group.id).flatMap(isExist => {
          if (isExist.isSuccess) {
            if (isExist.result) {
              ErrorMessages.error_GroupExists.toFutureFail
            } else {
              EsClient.esClient.execute {
                indexInto(Group.Index).doc(group).id(group.id).refresh(RefreshPolicy.WAIT_FOR)
              }.map(toIndexDocResponse(_))
            }
          } else {
            ErrorMessages.error_EsRequestFail(isExist).toFutureFail
          }
        })
      } else {
        EsClient.esClient.execute {
          indexInto(Group.Index).doc(group).id(group.id).refresh(RefreshPolicy.WAIT_FOR)
        }.map(toIndexDocResponse(_))
      }
    }
  }

  def deleteGroup(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      IndexService.deleteByGroupOrProject(groupRelatedIndexes, id, null).flatMap(idxRes => {
        EsClient.esClient.execute {
          delete(id).from(Group.Index).refresh(RefreshPolicy.WAIT_FOR)
        }.map(_ => idxRes)
      })
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(Group.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def getMaxGroups(): Future[Seq[Group]] = {
    EsClient.esClient.execute {
      search(Group.Index)
        .query(matchAllQuery())
        .limit(EsConfig.MaxCount)
        .sourceInclude(FieldKeys.FIELD_ID, FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION, FieldKeys.FIELD_AVATAR)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }.map(res => {
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          Nil
        } else {
          res.result.hits.hits.toIndexedSeq.map(hit => JacksonSupport.parse(hit.sourceAsString, classOf[Group]))
        }
      } else {
        throw RequestFailException(res.error.reason)
      }
    })
  }

  def updateGroup(group: Group): Future[UpdateDocResponse] = {
    if (null == group || null == group.id) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(group.id).in(Group.Index).doc(group.toUpdateMap)
      }
    }.map(toUpdateDocResponse(_))
  }

  def docExists(id: String) = {
    EsClient.esClient.execute {
      exists(id, Group.Index)
    }
  }

  def queryGroup(query: QueryGroup) = {
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.id)) esQueries += wildcardQuery(FieldKeys.FIELD_ID, s"*${query.id}*")
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      sortFields = Nil
    }
    EsClient.esClient.execute {
      search(Group.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(sortFields)
        .sourceInclude(defaultIncludeFields :+ FieldKeys.FIELD_ID :+ FieldKeys.FIELD_AVATAR)
    }
  }

  def getByIdsAsRawMap(ids: Iterable[String]) = {
    if (null != ids && ids.nonEmpty) {
      EsClient.esClient.execute {
        search(Group.Index).query(idsQuery(ids)).size(ids.size)
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }
}
