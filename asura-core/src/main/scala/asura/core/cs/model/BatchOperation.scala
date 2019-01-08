package asura.core.cs.model

import asura.core.es.model.LabelRef


object BatchOperation {

  case class BatchTransfer(group: String, project: String, ids: Seq[String])

  case class BatchOperationLabels(
                                   labels: Seq[UpdateLabels]
                                 )

  case class UpdateLabels(id: String, labels: Seq[LabelRef])

}
