package asura.core.assertion


import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

object Gte extends CompareOperator with Assertion {

  override val name: String = Assertions.GTE

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(src: Any, target: Any): AssertResult = {
    compareTwo(src, target) { (src, target) =>
      src.compareTo(target) >= 0
    }
  }
}
