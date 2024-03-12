import BuildVars._

name := "atom-publisher-lib"

// for testing dynamodb access
dynamoDBLocalDownloadDir := file(".dynamodb-local")
startDynamoDBLocal := startDynamoDBLocal.dependsOn(Test / compile).value
Test / test := (Test / test).dependsOn(startDynamoDBLocal).value
Test / testOptions += dynamoDBLocalTestCleanup.value

dependencyOverrides += "org.apache.thrift" % "libthrift" % "0.15.0"
dependencyOverrides += "com.twitter" %% "scrooge-core" % scroogeVersion
dependencyOverrides += "com.twitter" %% "scrooge-serializer" % scroogeVersion

libraryDependencies ++= Seq(
  "org.scala-lang.modules"     %% "scala-collection-compat" % "2.11.0",
  "org.typelevel"              %% "cats-core"             % "2.9.0",
  "io.circe"                   %% "circe-parser"          % "0.14.6",
  "com.gu"                     %% "fezziwig"              % "1.9.2",
  "com.gu"                     %% "content-atom-model"    % contentAtomVersion,
  "com.amazonaws"              %  "aws-java-sdk-dynamodb" % awsVersion,
  "com.amazonaws"              %  "aws-java-sdk-kinesis"  % awsVersion,
  "com.typesafe.scala-logging" %% "scala-logging"         % "3.9.5",
  "com.twitter"                %% "scrooge-serializer"    % scroogeVersion,
  "com.twitter"                %% "scrooge-core"          % scroogeVersion,
  "org.mockito"                %  "mockito-core"          % mockitoVersion % Test,
  "org.scalatestplus"          %% "mockito-4-6"           % "3.2.15.0" % Test,
  "org.scalatest"              %% "scalatest"             % "3.2.18" % Test
)
