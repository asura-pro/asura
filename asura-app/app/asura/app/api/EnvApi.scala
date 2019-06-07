package asura.app.api

import asura.app.AppErrorMessages
import asura.common.model.{ApiRes, ApiResError}
import asura.core.ErrorMessages
import asura.core.auth.AuthManager
import asura.core.es.EsResponse
import asura.core.es.model.Environment
import asura.core.es.service.{EnvironmentService, HttpCaseRequestService, JobService, ScenarioService}
import asura.core.model.QueryEnv
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnvApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    EnvironmentService.getEnvById(id).map(env => {
      if (null != env.auth && env.auth.nonEmpty) {
        env.auth.foreach(authData => {
          AuthManager(authData.`type`).foreach(_.mask(authData))
        })
      }
      env
    }).toOkResult
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    val env = req.bodyAs(classOf[Environment])
    env.fillCommonFields(getProfileId())
    EnvironmentService.index(env).toOkResult
  }

  def update(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    val env = req.bodyAs(classOf[Environment])
    env.fillCommonFields(getProfileId())
    EnvironmentService.updateEnv(id, env).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryEnv = req.bodyAs(classOf[QueryEnv])
    EnvironmentService.queryEnv(queryEnv).toOkResultByEsList()
  }

  def getAllAuth() = Action {
    OkApiRes(ApiRes(data = AuthManager.getAll()))
  }

  def delete(id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    val ids = Seq(id)
    val res = for {
      c <- HttpCaseRequestService.containEnv(ids)
      s <- ScenarioService.containEnv(ids)
      j <- JobService.containEnv(ids)
    } yield (c, s, j)
    res.flatMap(resTriple => {
      val (caseRes, scenarioRes, jobRes) = resTriple
      if (caseRes.isSuccess && scenarioRes.isSuccess && jobRes.isSuccess) {
        if (preview.nonEmpty && preview.get) {
          Future.successful(toActionResultFromAny(Map(
            "case" -> EsResponse.toApiData(caseRes.result),
            "scenario" -> EsResponse.toApiData(scenarioRes.result),
            "job" -> EsResponse.toApiData(jobRes.result)
          )))
        } else {
          if (caseRes.result.isEmpty && scenarioRes.result.isEmpty && jobRes.result.isEmpty) {
            EnvironmentService.deleteDoc(id).toOkResult
          } else {
            Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_CantDeleteEnv))))
          }
        }
      } else {
        val errorRes = if (!scenarioRes.isSuccess) scenarioRes else jobRes
        ErrorMessages.error_EsRequestFail(errorRes).toFutureFail
      }
    })
  }
}
