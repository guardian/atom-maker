import BuildVars._
import sbtrelease._
import ReleaseTransformations._


Sonatype.sonatypeSettings

name := "atom-publisher-lib"

organization := "com.gu"
scalaVersion := "2.12.16"

// for testing dynamodb access
dynamoDBLocalDownloadDir := file(".dynamodb-local")
startDynamoDBLocal := startDynamoDBLocal.dependsOn(compile in Test).value
test in Test := (test in Test).dependsOn(startDynamoDBLocal)
testOptions in Test += dynamoDBLocalTestCleanup.value

dependencyOverrides += "org.apache.thrift" % "libthrift" % "0.10.0"
dependencyOverrides += "com.twitter" %% "scrooge-core" % scroogeVersion
dependencyOverrides += "com.twitter" %% "scrooge-serializer" % scroogeVersion
// Necessary because of a conflict between catz, imported by scanamo 1.0.0M9, and scanamo-scrooge
dependencyOverrides += "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4"


libraryDependencies ++= Seq(
  "org.typelevel"              %% "cats-core"            % "1.5.0",
  "io.circe"                   %% "circe-parser"         % "0.11.0",
  "com.gu"                     %% "fezziwig"             % "1.1",
  "com.gu"                     %% "content-atom-model"   % contentAtomVersion,
  "com.amazonaws"              %  "aws-java-sdk-kinesis" % awsVersion,
  "com.typesafe.scala-logging" %% "scala-logging"        % "3.5.0",
  "com.twitter"                %% "scrooge-serializer"   % scroogeVersion,
  "com.twitter"                %% "scrooge-core"         % scroogeVersion,
  "com.typesafe.akka"          %% "akka-actor"           % akkaVersion,
  "org.scalatest"              %% "scalatest"            % "3.2.12" % "test",
  "org.scalatestplus"          %% "mockito-3-3"          % "3.1.2.0" % "test",
  "com.typesafe.akka"          %% "akka-testkit"         % akkaVersion % "test"
) ++  scanamoDeps

