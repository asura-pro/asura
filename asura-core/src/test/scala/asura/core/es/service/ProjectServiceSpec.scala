package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.Project
import asura.core.es.{EsClient, EsClientConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._


class ProjectServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  test("delete-index") {
    EsClient.httpClient.execute {
      deleteIndex(Project.Index)
    }.await match {
      case Right(res) =>
        println(res)
      case _ =>
    }
  }

  test("create-index") {
    val isOk = IndexService.initCheck(Project)
    assertResult(true)(isOk)
  }
}
