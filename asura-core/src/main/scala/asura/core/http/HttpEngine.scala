package asura.core.http

import java.security.cert.X509Certificate

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, RequestTimeoutException}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpsConnectionContext, UseHttp2}
import asura.common.model.{ApiMsg, BoolErrorRes, BoolErrorTypeRes}
import asura.common.util.LogUtils
import asura.core.CoreConfig
import asura.core.CoreConfig._
import asura.core.protocols.Protocols
import asura.core.util.JacksonSupport
import com.typesafe.scalalogging.Logger
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future, Promise}

object HttpEngine {

  val logger = Logger("HttpEngine")
  val http = Http()

  /**
   * This method will use linkerd proxy and set a `Host` header of original `host` default.
   * If the schema is `https`, it will replaced by `http` to make the linkerd handle https traffic.
   **/
  private def buildProxyRequest(request: HttpRequest, proxyServerTag: String): Future[HttpRequest] = {
    Future.successful {
      // do not need to do the validation
      val proxyServer = CoreConfig.linkerdConfig.servers.find(_.tag.equals(proxyServerTag)).get
      val proxyHost = proxyServer.proxyHost
      val httpProxyPort = proxyServer.httpProxyPort
      val httpsProxyPort = proxyServer.httpsProxyPort
      val originUri = request.uri
      val proxyRequest = if (Protocols.HTTPS.equals(request.uri.scheme)) {
        val proxyUri = originUri.withScheme(Protocols.HTTP).withAuthority(proxyHost, httpsProxyPort)
        request.withUri(proxyUri)
      } else {
        val proxyUri = originUri.withAuthority(proxyHost, httpProxyPort)
        request.withUri(proxyUri)
      }
      // val host = originUri.authority.host.address()
      // val hostHeader: HttpHeader = HttpHeader.parse("Host", host) match {
      //   case Ok(header: HttpHeader, errors: List[ErrorInfo]) =>
      //     if (errors.nonEmpty) logger.warn(errors.mkString(","))
      //     header
      //   case Error(error: ErrorInfo) =>
      //     logger.warn(error.detail)
      //     null
      // }
      // val proxyHeaders = if (null != hostHeader) proxyRequest.headers :+ hostHeader else proxyRequest.headers
      proxyRequest
    }
  }

  def singleRequestWithProxy(request: HttpRequest, proxyServerTag: String): Future[HttpResponse] = {
    buildProxyRequest(request, proxyServerTag).flatMap(request => singleRequest(request))
  }

  def singleRequestWithProxy[T >: Null <: AnyRef](
                                                   request: HttpRequest,
                                                   proxyServerTag: String,
                                                   clazz: Class[T]
                                                 ): Future[T] = {
    buildProxyRequest(request, proxyServerTag).flatMap(request => singleRequest(request, clazz))
  }

  val badSslConfig: AkkaSSLConfig = AkkaSSLConfig().mapSettings(s =>
    s.withLoose(
      s.loose
        .withAcceptAnyCertificate(true)
        .withDisableHostnameVerification(true)
    )
  )
  val ctx = http.createClientHttpsContext(badSslConfig)

  def singleRequest(request: HttpRequest): Future[HttpResponse] = {
    if (Protocols.HTTPS.equals(request.uri.scheme)) {
      val httpsCtx = new HttpsConnectionContext(
        trustfulSslContext,
        ctx.sslConfig,
        ctx.enabledCipherSuites,
        ctx.enabledProtocols,
        ctx.clientAuth,
        ctx.sslParameters,
        UseHttp2.Negotiated
      )
      http.singleRequest(request, httpsCtx)
    } else {
      http.singleRequest(request)
    }
  }

  def singleRequestStr(request: HttpRequest): Future[String] = {
    http.singleRequest(request).flatMap(res =>
      Unmarshal(res.entity).to[String]
    )
  }

  def singleRequest[T >: Null <: AnyRef](request: HttpRequest, clazz: Class[T]): Future[T] = {
    http.singleRequest(request).flatMap(res =>
      Unmarshal(res.entity).to[String].map(str =>
        JacksonSupport.parse(str, clazz)
      )
    )
  }

  // will block current thread
  def getStringBlock(request: HttpRequest): BoolErrorRes = {
    try {
      val futureResponse = http.singleRequest(request)
      val response = Await.result(futureResponse, Duration.Inf)
      val futureStr = Unmarshal(response.entity).to[String]
      (true, Await.result(futureStr, Duration.Inf))
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        (false, t.getMessage)
    }
  }

  // will block current thread
  def getResponseBlock[T >: Null <: AnyRef](request: HttpRequest, clazz: Class[T]): BoolErrorTypeRes[T] = {
    try {
      val futureResponse = http.singleRequest(request)
      val response = Await.result(futureResponse, Duration.Inf)
      val futureStr = Unmarshal(response.entity).to[String]
      val str = Await.result(futureStr, Duration.Inf)
      (true, ApiMsg.SUCCESS, JacksonSupport.parse(str, clazz))
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        (false, t.getMessage, null)
    }
  }

  private val trustfulSslContext: SSLContext = {

    object NoCheckX509TrustManager extends X509TrustManager {
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String) = ()

      override def checkServerTrusted(chain: Array[X509Certificate], authType: String) = ()

      override def getAcceptedIssuers = Array[X509Certificate]()
    }
    val context = SSLContext.getInstance("TLS")
    context.init(Array[KeyManager](), Array(NoCheckX509TrustManager), null)
    context
  }

  // code from: https://github.com/akka/akka-http/issues/622
  def singleRequestWithTimeout(request: HttpRequest, responseTimeout: FiniteDuration): Future[HttpResponse] = {
    val response = singleRequest(request)
    val promise = Promise[HttpResponse]()
    // stacktrace which point to here
    val timeoutException = RequestTimeoutException(request, s"Response did not arrive within ${responseTimeout}")
    val timeoutCancellable = system.scheduler.scheduleOnce(responseTimeout, new Runnable {
      override def run(): Unit = promise.tryFailure(timeoutException)
    })
    response.onComplete {
      case scala.util.Success(res) =>
        timeoutCancellable.cancel()
        try promise.success(res) catch {
          case _: Throwable => res.discardEntityBytes()
        }
      case scala.util.Failure(t) =>
        timeoutCancellable.cancel()
        promise.tryFailure(t)
    }
    promise.completeWith(response.map { res =>
      timeoutCancellable.cancel()
      res
    })
    promise.future
  }
}
