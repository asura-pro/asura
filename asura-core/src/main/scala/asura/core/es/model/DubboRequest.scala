package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class DubboRequest(
                         val summary: String,
                         val description: String,
                         val group: String,
                         val project: String,
                         val dubboGroup: String,
                         val interface: String,
                         val method: String,
                         val parameterTypes: Array[String],
                         val args: Array[Object],
                         val address: String,
                         val port: Int,
                         val version: String,
                         val assert: Map[String, Any],
                         var creator: String = null,
                         var createdAt: String = null,
                         var updatedAt: String = null,
                       ) extends BaseIndex {

  override def toUpdateScriptParams: (String, Map[String, Any]) = {
    val sb = StringBuilder.newBuilder
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m, sb)
    if (null != dubboGroup) {
      m += (FieldKeys.FIELD_DUBBO_GROUP -> dubboGroup)
      addScriptUpdateItem(sb, FieldKeys.FIELD_DUBBO_GROUP)
    }
    if (null != interface) {
      m += (FieldKeys.FIELD_INTERFACE -> interface)
      addScriptUpdateItem(sb, FieldKeys.FIELD_INTERFACE)
    }
    if (null != method) {
      m += (FieldKeys.FIELD_METHOD -> method)
      addScriptUpdateItem(sb, FieldKeys.FIELD_METHOD)
    }
    if (null != parameterTypes) {
      m += (FieldKeys.FIELD_PARAMETER_TYPES -> parameterTypes)
      addScriptUpdateItem(sb, FieldKeys.FIELD_PARAMETER_TYPES)
    } else {
      m += (FieldKeys.FIELD_PARAMETER_TYPES -> Nil)
      addScriptUpdateItem(sb, FieldKeys.FIELD_PARAMETER_TYPES)
    }
    if (null != args) {
      m += (FieldKeys.FIELD_ARGS -> args)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ARGS)
    } else {
      m += (FieldKeys.FIELD_ARGS -> Nil)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ARGS)
    }
    if (null != address) {
      m += (FieldKeys.FIELD_ADDRESS -> address)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ADDRESS)
    }
    if (port > 0) {
      m += (FieldKeys.FIELD_PORT -> port)
      addScriptUpdateItem(sb, FieldKeys.FIELD_PORT)
    }
    if (null != version) {
      m += (FieldKeys.FIELD_VERSION -> version)
      addScriptUpdateItem(sb, FieldKeys.FIELD_VERSION)
    }
    (sb.toString, m.toMap)
  }
}

object DubboRequest extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}dubbo-request"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_DUBBO_GROUP),
      KeywordField(name = FieldKeys.FIELD_INTERFACE),
      KeywordField(name = FieldKeys.FIELD_METHOD),
      NestedField(name = FieldKeys.FIELD_PARAMETER_TYPES, dynamic = Some("false")),
      NestedField(name = FieldKeys.FIELD_ARGS, dynamic = Some("false")),
      KeywordField(name = FieldKeys.FIELD_ADDRESS),
      BasicField(name = FieldKeys.FIELD_PORT, `type` = "integer"),
      KeywordField(name = FieldKeys.FIELD_VERSION),
      ObjectField(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
    )
  )
}

