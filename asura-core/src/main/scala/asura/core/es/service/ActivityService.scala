package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model._
import asura.core.es.service.BaseAggregationService._
import asura.core.model.{AggsItem, AggsQuery, SearchAfterActivity}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.DateHistogramInterval
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ActivityService extends CommonService with BaseAggregationService {

  val recentProjectScript = s"doc['${FieldKeys.FIELD_GROUP}'].value + '/' + doc['${FieldKeys.FIELD_PROJECT}'].value"
  val feedTypes = Seq(
    Activity.TYPE_NEW_USER, Activity.TYPE_NEW_CASE, Activity.TYPE_TEST_CASE, Activity.TYPE_NEW_GROUP,
    Activity.TYPE_NEW_PROJECT, Activity.TYPE_NEW_SCENARIO, Activity.TYPE_TEST_SCENARIO,
    Activity.TYPE_NEW_JOB, Activity.TYPE_TEST_JOB, Activity.TYPE_NEW_DUBBO, Activity.TYPE_TEST_DUBBO,
    Activity.TYPE_NEW_SQL, Activity.TYPE_TEST_SQL
  )

  def index(items: Seq[Activity]): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(Activity.Index).doc(item))
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def trend(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, true)
    val termsField = aggs.aggTermsField()
    val dateHistogram = dateHistogramAgg(aggsTermsName, FieldKeys.FIELD_TIMESTAMP)
      .fixedInterval(DateHistogramInterval.fromString(aggs.aggInterval()))
      .format("yyyy-MM-dd")
      .subAggregations(termsAgg(aggsTermsName, if (termsField.equals("creator")) FieldKeys.FIELD_USER else termsField).size(aggs.pageSize()))
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(dateHistogram)
    }.map(toAggItems(_, null, termsField))
  }

  def aggTerms(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, true)
    val aggField = aggs.aggField()
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(termsAgg(aggsTermsName, aggField).size(aggs.pageSize()))
    }.map(toAggItems(_, aggField, null))
  }

  def recentProjects(user: String, me: Boolean = true, wd: String = null, size: Int = 20, excludeGPs: Seq[(String, String)] = Nil): Future[Seq[AggsItem]] = {
    val esQueries = ArrayBuffer[Query]()
    if (me) {
      esQueries += termQuery(FieldKeys.FIELD_USER, user)
    } else {
      esQueries += not(termQuery(FieldKeys.FIELD_USER, user))
      // from a month ago
      esQueries += rangeQuery(FieldKeys.FIELD_TIMESTAMP).gte("now-30d")
    }
    esQueries += not(termsQuery(FieldKeys.FIELD_TYPE, Seq(Activity.TYPE_NEW_USER, Activity.TYPE_USER_LOGIN)))
    if (excludeGPs.nonEmpty) {
      esQueries += not(should(excludeGPs.map(gp =>
        must(termQuery(FieldKeys.FIELD_GROUP, gp._1), termQuery(FieldKeys.FIELD_PROJECT, gp._2)))
      ))
    }
    if (StringUtils.isNotEmpty(wd)) {
      esQueries += should(
        wildcardQuery(FieldKeys.FIELD_GROUP, s"*${wd}*"),
        wildcardQuery(FieldKeys.FIELD_PROJECT, s"*${wd}*")
      )
    }
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(
          termsAggregation(aggsTermsName)
            .script(Script(recentProjectScript))
            .size(size)
        )
    }.map(toAggItems(_, null, null))
  }

  def searchFeed(query: SearchAfterActivity) = {
    val sortFields = Seq(FieldSort(FieldKeys.FIELD_TIMESTAMP).desc(), FieldSort(FieldKeys.FIELD__ID).desc())
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.user)) esQueries += termQuery(FieldKeys.FIELD_USER, query.user)
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    esQueries += should(termsQuery(FieldKeys.FIELD_TYPE, feedTypes))
    esQueries += not(termQuery(FieldKeys.FIELD_TARGET_ID, StringUtils.EMPTY))
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(query.pageSize)
        .searchAfter(query.toSearchAfterSort)
        .sortBy(sortFields)
    }.flatMap { res =>
      if (res.isSuccess) {
        val hits = res.result.hits
        val userIds = mutable.HashSet[String]()
        val groupIds = mutable.HashSet[String]()
        val projectIds = mutable.HashSet[String]()
        val httpIds = mutable.HashSet[String]()
        val dubboIds = mutable.HashSet[String]()
        val sqlIds = mutable.HashSet[String]()
        val scnIds = mutable.HashSet[String]()
        val jobIds = mutable.HashSet[String]()
        val list = ArrayBuffer[Any]()
        val dataMap = mutable.Map[String, Any]("total" -> hits.total.value, "list" -> list)
        for (i <- 0 until hits.hits.length) {
          val hit = hits.hits(i)
          val sourceMap = hit.sourceAsMap
          userIds += sourceMap.getOrElse(FieldKeys.FIELD_USER, StringUtils.EMPTY).asInstanceOf[String]
          val activityType = sourceMap.getOrElse(FieldKeys.FIELD_TYPE, StringUtils.EMPTY).asInstanceOf[String]
          val targetId = sourceMap.getOrElse(FieldKeys.FIELD_TARGET_ID, StringUtils.EMPTY).asInstanceOf[String]
          if (StringUtils.isNotEmpty(targetId)) {
            activityType match {
              case Activity.TYPE_NEW_USER => // userId already be added
              case Activity.TYPE_NEW_GROUP => groupIds += targetId
              case Activity.TYPE_NEW_PROJECT => projectIds += targetId
              case Activity.TYPE_NEW_CASE | Activity.TYPE_TEST_CASE => httpIds += targetId
              case Activity.TYPE_NEW_DUBBO | Activity.TYPE_TEST_DUBBO => dubboIds += targetId
              case Activity.TYPE_NEW_SQL | Activity.TYPE_TEST_SQL => sqlIds += targetId
              case Activity.TYPE_NEW_SCENARIO | Activity.TYPE_TEST_SCENARIO => scnIds += targetId
              case Activity.TYPE_NEW_JOB | Activity.TYPE_TEST_JOB => jobIds += targetId
              case _ => // ignore
            }
          }
          if (i != hits.hits.length - 1) {
            list += sourceMap
          } else {
            list += sourceMap + (FieldKeys.FIELD__SORT -> hit.sort.getOrElse(Nil))
          }
        }
        val futures = ArrayBuffer[Future[Any]]()
        futures += UserProfileService.getByIdsAsRawMap(userIds).map(m => dataMap += ("users" -> m))
        futures += GroupService.getByIdsAsRawMap(groupIds).map(m => dataMap += ("group" -> m))
        futures += ProjectService.getByIdsAsRawMap(projectIds).map(m => dataMap += ("project" -> m))
        futures += HttpCaseRequestService.getByIdsAsRawMap(httpIds).map(m => dataMap += ("http" -> m))
        futures += DubboRequestService.getByIdsAsRawMap(dubboIds).map(m => dataMap += ("dubbo" -> m))
        futures += SqlRequestService.getByIdsAsRawMap(sqlIds).map(m => dataMap += ("sql" -> m))
        futures += ScenarioService.getByIdsAsRawMap(scnIds).map(m => dataMap += ("scenario" -> m))
        futures += JobService.getByIdsAsRawMap(jobIds).map(m => dataMap += ("job" -> m))
        Future.sequence(futures).map(_ => dataMap)
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    }
  }
}
