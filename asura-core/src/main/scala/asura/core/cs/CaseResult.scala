package asura.core.cs

import akka.http.scaladsl.model.HttpResponse
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.cs.assertion.engine.{AssertionContext, Statistic}
import asura.core.es.model.Case

import scala.concurrent.Future

case class CaseResult(
                       var id: String,
                       var assert: Map[String, Any],
                       var context: java.util.Map[Any, Any],
                       var request: CaseRequest,
                       var response: CaseResponse,
                       var statis: Statistic = Statistic(),
                       var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP
                     )

object CaseResult {

  def failResult(id: String, cs: Case): CaseResult = {
    val result = CaseResult(
      id = id,
      assert = cs.assert,
      context = null,
      request = null,
      response = null
    )
    result.statis.isSuccessful = false
    result
  }

  def eval(
            id: String,
            response: HttpResponse,
            assert: Map[String, Any],
            context: CaseContext,
            request: CaseRequest,
          ): Future[CaseResult] = {
    val statistic = Statistic()
    AssertionContext.eval(assert, context.rawContext, statistic).map { assertResult =>
      CaseResult(
        id = id,
        assert = assert,
        context = context.rawContext,
        request = request,
        response = context.getCaseResponse(response.status.reason()),
        statis = statistic,
        result = assertResult
      )
    }
  }
}
