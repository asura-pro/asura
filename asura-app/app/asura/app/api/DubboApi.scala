package asura.app.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import asura.core.ErrorMessages
import asura.dubbo.GenericRequest
import asura.dubbo.actor.GenericServiceInvokerActor
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfaceMethodParams, GetInterfacesMessage, GetProvidersMessage}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class DubboApi @Inject()(
                          implicit val system: ActorSystem,
                          val exec: ExecutionContext,
                          val configuration: Configuration,
                          val controllerComponents: SecurityComponents,
                        ) extends BaseApi {

  implicit val timeout: Timeout = 30.seconds
  val dubboInvoker = system.actorOf(GenericServiceInvokerActor.props(), "dubbo-invoker")

  def getInterfaces() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[GetInterfacesMessage])
    (dubboInvoker ? msg).toOkResult
  }

  def getProviders() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[GetProvidersMessage])
    (dubboInvoker ? msg).toOkResult
  }

  def getParams() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[GetInterfaceMethodParams])
    (dubboInvoker ? msg).toOkResult
  }

  def test() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[GenericRequest])
    if (msg.validate()) {
      (dubboInvoker ? msg).toOkResult
    } else {
      ErrorMessages.error_InvalidRequestParameters.toFutureFail
    }
  }
}
