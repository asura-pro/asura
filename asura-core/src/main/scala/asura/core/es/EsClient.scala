package asura.core.es

import asura.common.util.StringUtils
import asura.core.es.model._
import asura.core.es.service.IndexService
import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties, NoOpHttpClientConfigCallback}
import com.sksamuel.elastic4s.mappings.Analysis
import com.typesafe.scalalogging.Logger
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback

object EsClient {

  val logger = Logger("EsClient")
  private var client: ElasticClient = _
  private var onlineLogClient: ElasticClient = _

  def esClient: ElasticClient = client

  def esOnlineLogClient = onlineLogClient

  /**
    * check if index exists, if not create
    */
  def init(useLocalNode: Boolean, url: String, dataDir: String): Boolean = {
    if (useLocalNode) {
      EsConfig.IK_ANALYZER = Analysis()
      if (StringUtils.isNotEmpty(url)) {
        client = ElasticClient(ElasticProperties(url))
      } else {
        val localNode = LocalNode("asura", dataDir)
        logger.info(s"start local es node: ${localNode.ipAndPort}")
        client = localNode.client(true)
      }
    } else {
      client = ElasticClient(ElasticProperties(url))
    }
    var isAllOk = true
    val indices: Seq[IndexSetting] = Seq(
      Case, RestApi, Job, Project, Environment,
      Group, JobReport, JobNotify, Scenario, UserProfile,
      Activity, DomainOnlineLog, ProjectApiCoverage, DomainOnlineConfig
    )
    for (index <- indices if isAllOk) {
      logger.info(s"check es index ${index.Index}")
      isAllOk = IndexService.initCheck(index)
    }
    isAllOk = IndexService.checkTemplate()
    isAllOk
  }

  def initOnlineLogClient(url: String): Unit = {
    if (StringUtils.isNotEmpty(url)) {
      onlineLogClient = ElasticClient(ElasticProperties(url), new CusRequestConfigCallback(), NoOpHttpClientConfigCallback)
    }
  }

  class CusRequestConfigCallback extends RequestConfigCallback {

    val connectionTimeout = 600000
    val socketTimeout = 600000

    override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
      // See https://github.com/elastic/elasticsearch/issues/24069
      // It's fixed in master now but still yet to release to 6.3.1
      requestConfigBuilder.setConnectionRequestTimeout(0)
        .setConnectTimeout(connectionTimeout)
        .setSocketTimeout(socketTimeout)
    }
  }

}
