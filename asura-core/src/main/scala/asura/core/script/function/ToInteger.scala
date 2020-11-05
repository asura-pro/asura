package asura.core.script.function

import com.fasterxml.jackson.annotation.JsonIgnore

import scala.concurrent.Future

case class ToInteger() extends TransformFunction {

  @JsonIgnore
  val ZERO = Integer.valueOf(0)
  override val name: String = "toInteger"
  override val description: String = "Try to transform string to a integer"

  override def apply(arg: Object): Future[Integer] = {
    Future.successful {
      if (null != arg) {
        try {
          java.lang.Integer.parseInt(arg.toString)
        } catch {
          case _: Throwable => ZERO
        }
      } else {
        ZERO
      }
    }
  }
}
