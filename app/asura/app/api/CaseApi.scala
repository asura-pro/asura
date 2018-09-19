package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiRes
import asura.core.cs.CaseRunner
import asura.core.cs.assertion.Assertions
import asura.core.cs.model.QueryCase
import asura.core.es.model.Case
import asura.core.es.service.CaseService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class CaseApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    CaseService.getById(id).toOkResultByEsOneDoc(id)
  }

  def delete(id: String) = Action.async { implicit req =>
    CaseService.deleteDoc(id).toOkResult
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[Case])
    cs.fillCommonFields(getProfileId())
    CaseService.index(cs).toOkResult
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[Case])
    CaseService.updateCs(id, cs).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryCase = req.bodyAs(classOf[QueryCase])
    CaseService.queryCase(queryCase).toOkResult
  }

  def test() = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[Case])
    CaseRunner.test("test", cs).toOkResult
  }

  def getAllAssertions() = Action {
    OkApiRes(ApiRes(data = Assertions.getAll()))
  }
}
