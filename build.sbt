import sbtrelease._
import ReleaseStateTransformations.*
import sbtversionpolicy.withsbtrelease.ReleaseVersion

name := "atom-maker-lib"

lazy val artifactProductionSettings = Seq(
  organization := "com.gu",
  scalaVersion := "2.13.17",
  licenses := Seq(License.Apache2),
  scalacOptions := Seq("-deprecation", "-feature", "-release:11")
)

lazy val atomPublisher = (project in file("./atom-publisher-lib"))
  .settings(
    name := "atom-publisher-lib",
    Test / publishArtifact := true,
    artifactProductionSettings
  )


lazy val atomManagerPlay = (project in file("./atom-manager-play-lib"))
  .dependsOn(atomPublisher % "test->test;compile->compile")
  .settings(
    name := "atom-manager-play-lib",
    Test / publishArtifact := true,
    artifactProductionSettings
  )

lazy val atomLibraries = (project in file("."))
  .aggregate(atomPublisher, atomManagerPlay).settings(
    publish / skip := true,
    releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion
    )
  )