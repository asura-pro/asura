package asura.core.http

import akka.http.scaladsl.model.{HttpMethods => AkkaHttpMethods, _}
import asura.core.ErrorMessages
import asura.core.auth.AuthManager
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{Authorization, HttpCaseRequest}
import asura.core.runtime.{RuntimeContext, RuntimeMetrics}

import scala.collection.immutable
import scala.concurrent.Future

object HttpParser {

  def toHttpRequest(cs: HttpCaseRequest, context: RuntimeContext)(implicit metrics: RuntimeMetrics): Future[HttpRequest] = {
    var method: HttpMethod = null
    val headers: immutable.Seq[HttpHeader] = HeaderUtils.toHeaders(cs, context)
    val request = cs.request
    if (null == request) {
      method = AkkaHttpMethods.GET
    } else {
      method = HttpMethods.toAkkaMethod(request.method)
    }
    val uri: Uri = UriUtils.toUri(cs, context)
    val entityFuture = if (AkkaHttpMethods.GET != method) {
      EntityUtils.toEntity(cs, context)
    } else {
      Future.successful(HttpEntity.Empty)
    }
    entityFuture.flatMap(entity => {
      val notAuthoredRequest = HttpRequest(method = method, uri = uri, headers = headers, entity = entity)
      metrics.renderRequestEnd()
      val authUsed: Seq[Authorization] = if (null != context.options && null != context.options.getUsedEnv()) {
        context.options.getUsedEnv().auth
      } else {
        Nil
      }
      if (null != authUsed && authUsed.nonEmpty) {
        metrics.renderAuthBegin()
        authUsed.foldLeft(Future.successful(notAuthoredRequest))((futureRequest, auth) => {
          for {
            initialAuthoredRequest <- futureRequest
            authoredRequest <- {
              val operator = AuthManager(auth.`type`)
              if (operator.nonEmpty) {
                operator.get.authorize(initialAuthoredRequest, auth)
              } else {
                ErrorMessages.error_NotRegisteredAuth(auth.`type`).toFutureFail
              }
            }
          } yield authoredRequest
        }).map(req => {
          metrics.renderAuthEnd()
          req
        })
      } else {
        Future.successful(notAuthoredRequest)
      }
    })
  }
}
