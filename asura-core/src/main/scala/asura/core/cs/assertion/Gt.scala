package asura.core.cs.assertion


import asura.core.cs.assertion.engine.AssertResult

import scala.concurrent.Future

object Gt extends CompareOperator with Assertion {

  override val name: String = Assertions.GT

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(src: Any, target: Any): AssertResult = {
    compareTwo(src, target) { (src, target) =>
      src.compareTo(target) > 0
    }
  }
}
