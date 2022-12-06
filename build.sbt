import sbtrelease._

import ReleaseStateTransformations._

val scala2_11 = "2.11.12"
val scala2_12 = "2.12.16"

name := "atom-maker-lib"

lazy val baseSettings = Seq(
  organization := "com.gu",
  scalaVersion := scala2_12,
  crossScalaVersions := Seq(scala2_11, scala2_12),
  licenses := Seq("Apache V2" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(ScmInfo(url("https://github.com/guardian/atom-maker"),
    "scm:git:git@github.com:guardian/atom-maker.git")),
  scalacOptions := Seq("-deprecation", "-feature"),
  // FIXME remove when 2.11 build/release is discontinued
  Test / fork := true
)

lazy val atomPublisher = (project in file("./atom-publisher-lib"))
  .settings(baseSettings: _*)
  .settings(
    organization := "com.gu",
    name := "atom-publisher-lib"
  )
  .settings(Test / publishArtifact := true)


lazy val atomManagerPlay = (project in file("./atom-manager-play-lib"))
  .settings(baseSettings: _*)
  .settings(
    organization := "com.gu",
    name := "atom-manager-play-lib"
  )
  .settings(Test / publishArtifact := true)
  .dependsOn(atomPublisher % "test->test;compile->compile")

lazy val atomLibraries = (project in file("."))
  .aggregate(atomPublisher, atomManagerPlay)
  .settings(baseSettings: _*).settings(
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("+publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)
