import Dependencies._
import sbt.Keys._
import sbt._

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation", "-language:postfixOps", "-language:higherKinds", "-language:implicitConversions")
ThisBuild / organization := "cc.akkaha"
ThisBuild / version := "0.8.0"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / maintainer := "ngxcorpio@gmail.com"

lazy val root = Project("asura", file("."))
  .dependsOn(app, cli)
  .aggregate(app, cli)

// Sub Projects
def asuraProjects(id: String) = Project(id, file(id))
  .settings(publishSettings: _*)

lazy val app = asuraProjects("asura-app")
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= appDependencies)
  .settings(releaseSettings: _*)
  .settings(publishArtifact in Compile := true)
  .dependsOn(
    play % "compile->compile;test->test",
    common % "compile->compile;test->test",
    cluster % "compile->compile;test->test",
    core % "compile->compile;test->test",
    namerd % "compile->compile;test->test",
    example % "compile->compile;test->test",
    dubbo % "compile->compile;test->test",
    kafka % "compile->compile;test->test",
    ui % "compile->compile;test->test",
  ).aggregate(play, common, cluster, core, namerd, dubbo, kafka, example)

lazy val example = asuraProjects("asura-example")
  .settings(libraryDependencies ++= Seq(guice))
  .dependsOn(core)

lazy val play = asuraProjects("asura-play")
  .settings(libraryDependencies ++= appPlayDeps)
  .dependsOn(common % "compile->compile;test->test")

lazy val common = asuraProjects("asura-common")
  .settings(libraryDependencies ++= commonDependencies)

lazy val cluster = asuraProjects("asura-cluster")
  .settings(libraryDependencies ++= clusterDependencies)
  .dependsOn(common % "compile->compile;test->test")

lazy val ui = asuraProjects("asura-ui")
  .settings(libraryDependencies ++= uiDependencies)
  .dependsOn(common % "compile->compile;test->test")

lazy val cli = asuraProjects("asura-ui-cli")
  .dependsOn(ui % "compile->compile;test->test")

lazy val core = asuraProjects("asura-core")
  .settings(libraryDependencies ++= coreDependencies)
  .dependsOn(
    common % "compile->compile;test->test",
    dubbo % "compile->compile;test->test",
    kafka % "compile->compile;test->test",
    ui % "compile->compile;test->test",
  )

lazy val namerd = asuraProjects("asura-namerd")
  .settings(libraryDependencies ++= namerdDependencies)
  .dependsOn(common % "compile->compile;test->test")

lazy val dubbo = asuraProjects("asura-dubbo")
  .settings(libraryDependencies ++= dubboDependencies)
  .dependsOn(common % "compile->compile;test->test")

lazy val kafka = asuraProjects("asura-kafka")
  .settings(libraryDependencies ++= kafkaDependencies)
  .dependsOn(common % "compile->compile;test->test")

// release
val username = "asura-pro"
val repo = "asura"

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

lazy val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    //runClean,
    // runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommand("publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    //pushChanges
  )
)
lazy val publishSettings = Seq(
  homepage := Some(url(s"https://github.com/$username/$repo")),
  licenses += "MIT" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
  apiURL := Some(url(s"https://$username.github.io/$repo/latest/api/")),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  developers := List(
    Developer(
      id = username,
      name = "zhengshaodong",
      email = "ngxcorpio@gmail.com",
      url = new URL(s"http://github.com/${username}")
    )
  ),
  useGpg := true,
  usePgpKeyHex("200BB242B4BE47DD"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
  // Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
)

coverageEnabled := false
