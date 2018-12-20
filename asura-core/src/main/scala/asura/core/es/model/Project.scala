package asura.core.es.model

import asura.core.es.EsConfig
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings.{KeywordField, MappingDefinition, NestedField}

import scala.collection.mutable

/** group and id should composite a unique key */
case class Project(
                    val id: String,
                    val summary: String,
                    val description: String,
                    val group: String,
                    val openapi: String = null,
                    val avatar: String = null,
                    val domains: Seq[LabelRef] = Nil, // online domain, should only one
                    var creator: String = null,
                    var createdAt: String = null,
                  ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != avatar) {
      m += (FieldKeys.FIELD_AVATAR -> avatar)
    }
    if (null != openapi) {
      m += (FieldKeys.FIELD_OPENAPI -> openapi)
    }
    if (null != domains) {
      m += (FieldKeys.FIELD_DOMAINS -> JacksonSupport.mapper.convertValue(domains, classOf[java.util.List[Map[String, Any]]]))
    }
    m.toMap
  }

  def generateDocId() = Project.generateDocId(group, id)
}

object Project extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}project"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_ID, copyTo = Seq(FieldKeys.FIELD__TEXT)),
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_OPENAPI, index = Option("false")),
      KeywordField(name = FieldKeys.FIELD_AVATAR, index = Option("false")),
      NestedField(name = FieldKeys.FIELD_DOMAINS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
    )
  )

  def generateDocId(group: String, projectId: String) = s"${group}_${projectId}"
}
