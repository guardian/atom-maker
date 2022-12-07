import sbtrelease._
import ReleaseStateTransformations._


name := "atom-maker-lib"

lazy val baseSettings = Seq(
  organization := "com.gu",
  scalaVersion := "2.12.16",
  licenses := Seq("Apache V2" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(ScmInfo(url("https://github.com/guardian/atom-maker"),
    "scm:git:git@github.com:guardian/atom-maker.git")),
  scalacOptions := Seq("-deprecation", "-feature"),
  publishTo := sonatypePublishToBundle.value,
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
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommand("publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)
