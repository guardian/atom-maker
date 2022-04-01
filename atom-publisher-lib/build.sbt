import BuildVars._
import com.typesafe.sbt.SbtPgp.autoImportImpl._
import sbtrelease._

import ReleaseStateTransformations._


Sonatype.sonatypeSettings

name := "atom-publisher-lib"

organization := "com.gu"
scalaVersion := "2.11.11"

// for testing dynamodb access
dynamoDBLocalDownloadDir := file(".dynamodb-local")
startDynamoDBLocal := startDynamoDBLocal.dependsOn(compile in Test).value
test in Test := (test in Test).dependsOn(startDynamoDBLocal)
testOptions in Test += dynamoDBLocalTestCleanup.value

dependencyOverrides += "org.apache.thrift" % "libthrift" % "0.10.0"
dependencyOverrides += "com.twitter" %% "scrooge-core" % scroogeVersion
dependencyOverrides += "com.twitter" %% "scrooge-serializer" % scroogeVersion

libraryDependencies ++= Seq(
  "org.typelevel"              %% "cats-core"            % "1.5.0",
  "io.circe"                   %% "circe-parser"         % "0.11.0",
  "com.gu"                     %% "fezziwig"             % "1.1",
  "com.gu"                     %% "content-atom-model"   % contentAtomVersion,
  "com.amazonaws"              %  "aws-java-sdk-kinesis" % awsVersion,
  "com.typesafe.scala-logging" %% "scala-logging"        % "3.4.0",
  "com.twitter"                %% "scrooge-serializer"   % scroogeVersion,
  "com.twitter"                %% "scrooge-core"         % scroogeVersion,
  "com.typesafe.akka"          %% "akka-actor"           % akkaVersion,
  "org.mockito"                %  "mockito-core"         % mockitoVersion % "test",
  "org.scalatest"              %% "scalatest"            % "2.2.6" % "test",
  "com.typesafe.akka"          %% "akka-testkit"         % akkaVersion % "test"
) ++  scanamoDeps
