import com.typesafe.sbt.SbtPgp.autoImportImpl._
import sbtrelease._

import ReleaseStateTransformations._

Sonatype.sonatypeSettings

name := "atom-maker-lib"

organization := "com.gu"

scalaVersion := "2.11.8"
scalaVersion in ThisBuild := "2.11.8"

val sonatypeReleaseSettings = Seq(
  licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(ScmInfo(url("https://github.com/guardian/atom-maker"),
  "scm:git:git@github.com:guardian/atom-maker.git")),
  pomExtra := (
    <url>https://github.com/guardian/atom-maker</url>
      <developers>
        <developer>
          <id>paulmr</id>
          <name>Paul Roberts</name>
          <url>https://github.com/paulmr</url>
        </developer>
      </developers>
    ),
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    )
)

lazy val atomPublisher = (project in file("./atom-publisher-lib"))
  .settings(
    organization := "com.gu",
    name := "atom-publisher-lib"
  )
  .settings(publishArtifact in Test := true)
  .settings(sonatypeReleaseSettings: _*)

lazy val atomManagerPlay = (project in file("./atom-manager-play-lib"))
  .settings(
    organization := "com.gu",
    name := "atom-manager-play-lib"
  )
  .settings(publishArtifact in Test := true)
  .settings(sonatypeReleaseSettings: _*)
  .dependsOn(atomPublisher % "test->test;compile->compile")

lazy val atomLibraries = (project in file("."))
  .settings (publishArtifact := false)
  .dependsOn(atomPublisher, atomManagerPlay % "test->test;compile->compile")
  .aggregate(atomPublisher, atomManagerPlay)
