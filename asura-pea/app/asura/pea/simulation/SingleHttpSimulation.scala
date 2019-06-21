package asura.pea.simulation

import asura.pea.model.{During, Injection, SingleRequest}
import asura.pea.singleHttpScenario
import io.gatling.core.Predef._
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._

class SingleHttpSimulation extends Simulation {

  val scn = scenario(singleHttpScenario.name)
    .exec(toAction(singleHttpScenario.request))

  setUp(
    scn.inject(
      getInjectionStep()
    ).protocols(http)
  )

  def getInjectionStep(): OpenInjectionStep = {
    val injection = singleHttpScenario.injection
    val during = injection.during
    injection.`type` match {
      case Injection.TYPE_RAMP_USERS => rampUsers(injection.users) during (toFiniteDuration(during))
      case Injection.TYPE_HEAVISIDE_USERS => heavisideUsers(injection.users) during (toFiniteDuration(during))
      case Injection.TYPE_AT_ONCE_USERS => atOnceUsers(injection.users)
      case Injection.TYPE_CONSTANT_USERS_PER_SEC => constantUsersPerSec(injection.users) during (toFiniteDuration(during))
      case Injection.TYPE_RAMP_USERS_PER_SEC => rampUsersPerSec(injection.users) to injection.to during (toFiniteDuration(during))
    }
  }

  def toFiniteDuration(during: During): FiniteDuration = {
    during.unit match {
      case During.TIME_UNIT_MILLI => during.value millis
      case During.TIME_UNIT_SECOND => during.value seconds
      case During.TIME_UNIT_MINUTE => during.value minutes
      case During.TIME_UNIT_HOUR => during.value hours
    }
  }

  def toAction(request: SingleRequest): HttpRequestBuilder = {
    http(request.name)
      .httpRequest(request.method, request.url)
      .headers(request.getHeaders())
      .body(StringBody(request.getBody()))
  }
}
