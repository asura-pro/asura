package asura.app.api.model

import asura.core.cs.ContextOptions
import asura.core.es.model.Case

case class TestCase(id: String, cs: Case, options: ContextOptions)
