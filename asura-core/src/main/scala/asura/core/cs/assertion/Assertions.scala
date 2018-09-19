package asura.core.cs.assertion

import scala.collection.mutable

object Assertions {

  // comparison
  val EQ = "$eq"
  val NE = "$ne"
  val GT = "$gt"
  val GTE = "$gte"
  val LT = "$lt"
  val LTE = "$lte"
  val IN = "$in"
  val NIN = "$nin"
  val REGEX = "$regex"
  // logical
  val AND = "$and"
  val NOT = "$not"
  val NOR = "$nor"
  val OR = "$or"
  // element
  val TYPE = "$type"
  // array
  val SIZE = "$size"
  // script
  val SCRIPT = "$script"

  private val assertions = mutable.HashMap[String, Assertion]()

  // simple have explicit expect and actual value
  val normals = Seq(
    Eq, Gt, Gte, In, Lt, Lte, Ne, Nin, Regex, Size, Type
  )
  // logic or complex computation
  val specials = Seq(
    And, Nor, Not, Or, Script
  )
  normals.foreach(register(_))
  specials.foreach(register(_))

  /** this is not thread safe */
  def register(assertion: Assertion): Unit = {
    assertions += (assertion.name -> assertion)
  }

  def get(name: String): Option[Assertion] = assertions.get(name)

  def getAll() = normals
}
