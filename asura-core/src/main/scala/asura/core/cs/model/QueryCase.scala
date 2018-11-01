package asura.core.cs.model

case class QueryCase(
                      group: String,
                      project: String,
                      path: String,
                      methods: Seq[String],
                      text: String,
                      ids: Seq[String],
                      labels: Seq[String],
                      hasCreators: Boolean = false,
                    ) extends QueryPage
