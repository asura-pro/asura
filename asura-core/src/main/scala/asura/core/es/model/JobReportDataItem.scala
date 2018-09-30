package asura.core.es.model

import asura.core.cs.{CaseRequest, CaseResponse}
import asura.core.es.EsConfig
import asura.core.es.model.JobReportData.CaseReportItemMetrics
import com.sksamuel.elastic4s.mappings._

case class JobReportDataItem(
                              val reportId: String,
                              val caseId: String,
                              val scenarioId: String,
                              val jobId: String,
                              var metrics: CaseReportItemMetrics,
                              val request: CaseRequest,
                              val response: CaseResponse,
                              var assertions: Map[String, Any],
                              var assertionsResult: java.util.Map[_, _]
                            ) {

}

object JobReportDataItem extends IndexSetting {

  val INDEX_DATE_TIME_PATTERN = "yyyy.MM.dd"
  val Index: String = s"${EsConfig.IndexPrefix}job-report-item"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_REPORT_ID),
      KeywordField(name = FieldKeys.FIELD_JOB_ID),
      KeywordField(name = FieldKeys.FIELD_CASE_ID),
      KeywordField(name = FieldKeys.FIELD_SCENARIO_ID),
      ObjectField(name = FieldKeys.FIELD_METRICS, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_REQUEST, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_RESPONSE, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_ASSERTIONS, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_ASSERTIONS_RESULT, dynamic = Some("false")),
    )
  )
}
