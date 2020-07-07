import play.sbt.PlayImport.{ehcache, filters, guice, ws}
import sbt._

object Dependencies {

  val akkaVersion = "2.6.3"
  val akkaHttpVersion = "10.1.11"

  private val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  private val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  private val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaMetrics = "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  private val akkaHttpXml = "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion
  private val config = "com.typesafe" % "config" % "1.4.0"
  private val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.0"
  private val guava = "com.google.guava" % "guava" % "23.6-jre"
  private val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
  private val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.7"
  private val quartz = "org.quartz-scheduler" % "quartz" % "2.3.0" excludeAll(ExclusionRule(organization = "com.mchange", name = "c3p0"), ExclusionRule(organization = "com.mchange", name = "mchange-commons-java"))
  private val swaggerParser = "io.swagger" % "swagger-parser" % "1.0.33"
  private val elastic4s = "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.5.0"
  private val joddCore = "org.jodd" % "jodd-core" % "3.9.1"
  private val jsonPath = "com.jayway.jsonpath" % "json-path" % "2.4.0"

  // kafka
  private val akkaKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.4"
  private val kafkaAvroSerializer = "io.confluent" % "kafka-avro-serializer" % "4.0.0" excludeAll (ExclusionRule(organization = "org.slf4j", name = "*"))

  // dubbo, specify javassist and jbossnetty deps because of coursier dep resolve problems
  private val dubbo = "com.alibaba" % "dubbo" % "2.6.5" excludeAll(ExclusionRule(organization = "org.springframework"), ExclusionRule(organization = "org.javassist"), ExclusionRule(organization = "org.jboss.netty"))
  private val dubboJavassist = "org.javassist" % "javassist" % "3.21.0-GA"
  private val dubboJbossNetty = "org.jboss.netty" % "netty" % "3.2.5.Final"
  private val dubboSpring = "org.springframework" % "spring-context" % "4.3.10.RELEASE" % Test
  private val curator = "org.apache.curator" % "curator-recipes" % "2.12.0"

  // Database
  private val mysqlConnector = "mysql" % "mysql-connector-java" % "5.1.23"
  private val redisson = "org.redisson" % "redisson" % "3.5.7"
  private val druid = "com.alibaba" % "druid" % "1.1.16"

  // Log dependencies
  private val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
  private val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  private val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  // Test dependencies
  private val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  private val scalaTestDeps = Seq(
    "org.scalactic" %% "scalactic" % "3.1.1",
    "org.scalatest" %% "scalatest" % "3.1.1" % Test
  )
  private val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test

  private val loggingDeps = Seq(slf4jApi, logback, scalaLogging)
  private val httpDeps = Seq(akkaStream, akkaHttp, akkaHttpXml, jackson)
  private val databaseDeps = Seq(mysqlConnector)
  private val testDeps = Seq(akkaTestKit, akkaHttpTestkit) ++ scalaTestDeps

  val commonDependencies = Seq(akkaTestKit, config, akkaActor, jackson) ++ scalaTestDeps ++ loggingDeps
  val clusterDependencies = Seq(akkaCluster, akkaMetrics, akkaClusterTools) ++ commonDependencies
  val coreDependencies = Seq(
    commonsLang3, jackson, elastic4s, druid,
    joddCore, jsonPath, swaggerParser, quartz, redisson
  ) ++ commonDependencies ++ httpDeps ++ databaseDeps
  val appDependencies = Seq(config, guava, commonsCodec) ++ loggingDeps ++ httpDeps ++ testDeps
  val namerdDependencies = Seq(akkaStream, akkaHttp) ++ commonDependencies
  val dubboDependencies = Seq(dubbo, curator, dubboJavassist, dubboJbossNetty, dubboSpring, akkaStream) ++ commonDependencies
  val kafkaDependencies = Seq(akkaStream, akkaKafka, kafkaAvroSerializer) ++ commonDependencies

  val commonPlayDeps = Seq(
    guice,
    ehcache,
    ws,
    filters,
  )

  val playPac4jVersion = "10.0.0"
  val pac4jVersion = "4.0.0"
  val appPlayDeps = Seq(
    "org.webjars" % "swagger-ui" % "3.17.4",
    "org.webjars.npm" % "swagger-editor-dist" % "3.1.16",
    "org.pac4j" %% "play-pac4j" % playPac4jVersion,
    "org.pac4j" % "pac4j-http" % pac4jVersion,
    "org.pac4j" % "pac4j-ldap" % pac4jVersion,
    "org.pac4j" % "pac4j-cas" % pac4jVersion,
    "org.pac4j" % "pac4j-jwt" % pac4jVersion,
    "com.typesafe.play" %% "play-mailer" % "7.0.1",
    "com.typesafe.play" %% "play-mailer-guice" % "7.0.1",
    "com.typesafe.play" %% "play-json" % "2.7.4",
  ) ++ commonPlayDeps
}
