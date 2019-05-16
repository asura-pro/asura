package asura.core.es.model

/**
  *
  * @param srcPath     jsonpath expression, which represents the current response
  * @param dstName     variable name, this should be unique in the whole scope
  * @param scope       [[asura.core.runtime.RuntimeContext.KEY__G]],
  *                    [[asura.core.runtime.RuntimeContext.KEY__J]],
  *                    [[asura.core.runtime.RuntimeContext.KEY__S]],
  * @param description some description
  * @param enabled     enabled
  * @param function    transform function
  */
case class VariablesExportItem(
                                srcPath: String,
                                dstName: String,
                                scope: String,
                                description: String,
                                enabled: Boolean = true,
                                function: String = null,
                              )
