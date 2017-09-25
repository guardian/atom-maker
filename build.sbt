import com.typesafe.sbt.SbtPgp.autoImportImpl._
import sbtrelease._

import ReleaseStateTransformations._

name := "atom-maker-lib"

lazy val baseSettings = Seq(
  organization := "com.gu",
  scalaVersion := "2.11.11",
  licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(ScmInfo(url("https://github.com/guardian/atom-maker"),
    "scm:git:git@github.com:guardian/atom-maker.git"))
)

lazy val atomPublisher = (project in file("./atom-publisher-lib"))
  .settings(baseSettings: _*)
  .settings(
    organization := "com.gu",
    name := "atom-publisher-lib"
  )
  .settings(publishArtifact in Test := true)


lazy val atomManagerPlay = (project in file("./atom-manager-play-lib"))
  .settings(baseSettings: _*)
  .settings(
    organization := "com.gu",
    name := "atom-manager-play-lib"
  )
  .settings(publishArtifact in Test := true)
  .dependsOn(atomPublisher % "test->test;compile->compile")

lazy val atomLibraries = (project in file("."))
  .aggregate(atomPublisher, atomManagerPlay)
  .settings(baseSettings: _*).settings(
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)
