package asura.core.cs.assertion.engine

import akka.http.scaladsl.model.HttpResponse
import asura.core.cs.{CaseContext, CaseRequest, CaseResponse, CaseResult}
import asura.core.http.HeaderUtils
import asura.core.util.JsonPathUtils

import scala.concurrent.Future

object HttpResponseAssert {

  val KEY_STATUS = "status"
  val KEY_HEADERS = "headers"
  val KEY_ENTITY = "entity"

  /** use java type system because of json path library */
  def generateCaseReport(
                          caseId: String,
                          assert: Map[String, Any],
                          response: HttpResponse,
                          entity: String,
                          caseRequest: CaseRequest,
                          caseContext: CaseContext
                        ): Future[CaseResult] = {
    var isJson = false
    caseContext.setCurrentStatus(response.status.intValue())
    val headers = new java.util.HashMap[String, String]()
    response.headers.foreach(header => {
      headers.put(header.name(), header.value())
      if (HeaderUtils.isApplicationJson(header)) {
        isJson = true
      }
    })
    caseContext.setCurrentHeaders(headers)
    if (isJson) {
      val entityDoc = JsonPathUtils.parse(entity)
      caseContext.setCurrentEntity(entityDoc)
    } else {
      try {
        val entityDoc = JsonPathUtils.parse(entity)
        caseContext.setCurrentEntity(entityDoc)
      } catch {
        case _: Throwable =>
          caseContext.setCurrentEntity(entity)
      }
    }
    import scala.collection.JavaConverters.mapAsScalaMap
    val caseResponse = CaseResponse(response.status.intValue(), response.status.reason(), mapAsScalaMap(headers), entity)
    CaseResult.eval(caseId, response, assert, caseContext, caseRequest, caseResponse)
  }
}
