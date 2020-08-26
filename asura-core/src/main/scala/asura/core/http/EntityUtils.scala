package asura.core.http

import java.net.URLEncoder

import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, RequestEntity}
import akka.util.ByteString
import asura.common.util.{LogUtils, StringUtils}
import asura.core.es.model.{HttpCaseRequest, KeyValueObject}
import asura.core.http.UriUtils.UTF8
import asura.core.runtime.RuntimeContext
import asura.core.util.JacksonSupport
import com.fasterxml.jackson.core.`type`.TypeReference
import com.typesafe.scalalogging.Logger

object EntityUtils {

  val logger = Logger("EntityUtils")

  def toEntity(cs: HttpCaseRequest, context: RuntimeContext): RequestEntity = {
    val request = cs.request
    var contentType: ContentType = ContentTypes.`text/plain(UTF-8)`
    var byteString: ByteString = ByteString.empty
    if (StringUtils.isNotEmpty(request.contentType) && null != request.body && request.body.nonEmpty) {
      request.contentType match {
        case HttpContentTypes.JSON =>
          contentType = ContentTypes.`application/json`
          val body = request.body.find(_.contentType == HttpContentTypes.JSON)
          if (body.nonEmpty) {
            byteString = ByteString(context.renderTemplateAsString(body.get.data))
          }
        case HttpContentTypes.X_WWW_FORM_URLENCODED =>
          contentType = ContentTypes.`application/x-www-form-urlencoded`
          val body = request.body.find(_.contentType == HttpContentTypes.X_WWW_FORM_URLENCODED)
          if (body.nonEmpty) {
            var bodyStr: String = null
            try {
              val sb = new StringBuilder()
              val params = JacksonSupport.parse(body.get.data, new TypeReference[Seq[KeyValueObject]]() {})
              for (pair <- params if (pair.enabled && StringUtils.isNotEmpty(pair.key))) {
                val rendered = context.renderTemplateAsString(pair.value)
                sb.append(pair.key).append("=").append(URLEncoder.encode(rendered, UTF8)).append("&")
              }
              if (sb.nonEmpty) {
                sb.deleteCharAt(sb.length - 1)
              }
              bodyStr = sb.toString
            } catch {
              case t: Throwable =>
                val errLog = LogUtils.stackTraceToString(t)
                logger.warn(errLog)
                bodyStr = errLog
            }
            byteString = ByteString(bodyStr)
          }
        case HttpContentTypes.TEXT_PLAIN =>
          contentType = ContentTypes.`text/plain(UTF-8)`
          val body = request.body.find(_.contentType == HttpContentTypes.TEXT_PLAIN)
          if (body.nonEmpty) {
            byteString = ByteString(context.renderTemplateAsString(body.get.data))
          }
        case _ =>
      }
    }
    HttpEntity(contentType, byteString)
  }
}
