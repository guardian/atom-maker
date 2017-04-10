import BuildVars._
import com.typesafe.sbt.SbtPgp.autoImportImpl._
import sbtrelease._

import ReleaseStateTransformations._


Sonatype.sonatypeSettings

name := "atom-publisher-lib"

organization := "com.gu"
scalaVersion := "2.11.8"

// for testing dynamodb access
dynamoDBLocalDownloadDir := file(".dynamodb-local")
startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test)
test in Test <<= (test in Test).dependsOn(startDynamoDBLocal)
testOptions in Test <+= dynamoDBLocalTestCleanup

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "com.gu"                     %% "content-atom-model"       % contentAtomVersion,
  "com.amazonaws"              %  "aws-java-sdk-kinesis"     % awsVersion,
  "com.typesafe.scala-logging" %% "scala-logging"            % "3.4.0",
  "com.twitter"                %% "scrooge-serializer"       % scroogeVersion,
  "com.twitter"                %% "scrooge-core"             % scroogeVersion,
  "com.typesafe.akka"          %% "akka-actor"               % akkaVersion,
  "org.mockito"                %  "mockito-core"             % mockitoVersion % "test",
  "org.scalatest"              %% "scalatest"                % "2.2.6" % "test",
  "com.typesafe.akka"          %% "akka-testkit"             % akkaVersion % "test",
  "org.typelevel"              %% "cats-core"                % "0.9.0",
  "com.github.mpilquist"       %% "simulacrum"               % "0.10.0",
  "com.gu"                     % "content-atom-model-thrift" % "2.4.34"
) ++  scanamoDeps

thriftTransformThriftFiles := Seq(file("contentatom.thrift"))
thriftTransformChangeNamespace := { (orig: String) => orig.replaceFirst("content", "draftcontent")}