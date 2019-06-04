package asura.core.script.builtin

object StringGenerator {

  val random =
    """
      |var RandomStringUtils = Java.type('org.apache.commons.lang3.RandomStringUtils');
      |function random(count) { return RandomStringUtils.randomAlphanumeric(count);}
    """.stripMargin

  val randomAlphabetic =
    """
      |var RandomStringUtils = Java.type('org.apache.commons.lang3.RandomStringUtils');
      |function randomAlphabetic(count) { return RandomStringUtils.randomAlphabetic(count);}
    """.stripMargin

  val randomNumeric =
    """
      |var RandomStringUtils = Java.type('org.apache.commons.lang3.RandomStringUtils');
      |function randomNumeric(count) { return RandomStringUtils.randomNumeric(count);}
    """.stripMargin

  val UUID =
    """
      |var UUID = Java.type('java.util.UUID');
      |function uuid() { return UUID.randomUUID().toString();}
    """.stripMargin

  def exports = Seq(random, UUID, randomAlphabetic, randomNumeric).mkString
}
