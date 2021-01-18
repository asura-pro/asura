package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.model.{NewFile, NewFolder}
import asura.common.util.StringUtils
import asura.core.es.model.FileNode
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.FileNodeService
import asura.core.model.QueryFile
import asura.core.security.PermissionAuthProvider
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext

@Singleton
class FileNodeApi @Inject()(
                             implicit val system: ActorSystem,
                             val exec: ExecutionContext,
                             val configuration: Configuration,
                             val controllerComponents: SecurityComponents,
                             val permissionAuthProvider: PermissionAuthProvider,
                           ) extends BaseApi {

  def get(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      FileNodeService.getFileNodeById(id).toOkResult
    }
  }

  def newFile(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val q = req.bodyAs(classOf[NewFile])
      if (q != null && FileNode.isNameLegal(q.name) && StringUtils.isNotEmpty(q.app)) {
        val doc = FileNode(
          group = group,
          project = project,
          `type` = FileNode.TYPE_FILE,
          summary = q.name,
          description = q.description,
          parent = if (StringUtils.isNotEmpty(q.parent)) q.parent else null,
          path = if (StringUtils.isNotEmpty(q.parent) && q.path != null && q.path.nonEmpty) q.path else null,
          app = q.app,
          data = q.data,
        )
        doc.fillCommonFields(user)
        FileNodeService.index(doc).toOkResult
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

  def newFolder(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val q = req.bodyAs(classOf[NewFolder])
      if (q != null && FileNode.isNameLegal(q.name)) {
        val doc = FileNode(
          group = group,
          project = project,
          `type` = FileNode.TYPE_FOLDER,
          summary = q.name,
          description = q.description,
          parent = if (StringUtils.isNotEmpty(q.parent)) q.parent else null,
          path = if (StringUtils.isNotEmpty(q.parent) && q.path != null && q.path.nonEmpty) q.path else null,
        )
        doc.fillCommonFields(user)
        FileNodeService.index(doc).toOkResult
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val q = req.bodyAs(classOf[QueryFile])
      q.group = group
      q.project = q.project
      FileNodeService.queryDocs(q).toOkResultByEsList()
    }
  }

}
