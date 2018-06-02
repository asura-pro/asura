package asura.core.cs.operators

import asura.core.cs.asserts.AssertResult

object Eq extends CompareOperator {

  def apply(src: Any, target: Any): AssertResult = {
    compareTwo(src, target) { (src, target) =>
      src.compareTo(target) == 0
    }
  }
}
