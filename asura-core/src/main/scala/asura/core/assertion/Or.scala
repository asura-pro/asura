package asura.core.assertion

import asura.core.assertion.engine.{AssertResult, AssertionContext, FailAssertResult, Statistic}
import asura.core.concurrent.ExecutionContextManager.cachedExecutor

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

case class Or() extends Assertion {

  override val name: String = Assertions.OR

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Or.apply(actual, expect)
  }

}

object Or {

  def apply(actual: Any, except: Any): Future[AssertResult] = {
    val result = AssertResult(
      isSuccessful = false,
      msg = AssertResult.MSG_FAILED
    )
    val subResults = ArrayBuffer[mutable.Map[String, Any]]()
    result.subResult = subResults
    except match {
      case assertions: Seq[_] =>
        if (assertions.nonEmpty) {
          val assertionResults = assertions.map(assertion => {
            val subStatis = Statistic()
            val assertionMap = assertion.asInstanceOf[Map[String, Any]]
            val contextMap = actual.asInstanceOf[Object]
            AssertionContext.eval(assertionMap, contextMap, subStatis).map((subStatis, _))
          })
          Future.sequence(assertionResults).map(subStatisResults => {
            val subResults = ArrayBuffer[java.util.Map[String, Any]]()
            result.subResult = subResults
            subStatisResults.foreach(subStatisResult => {
              val (subStatis, subResult) = subStatisResult
              subResults += subResult
              result.pass(subStatis.passed)
              result.fail(subStatis.failed)
              if (subStatis.isSuccessful) {
                result.isSuccessful = true
                result.msg = AssertResult.MSG_PASSED
              }
            })
            result
          })
        } else {
          Future.successful(null)
        }
      case _ =>
        Future.successful(FailAssertResult(1, AssertResult.msgIncomparableTargetType(except)))
    }
  }

}
