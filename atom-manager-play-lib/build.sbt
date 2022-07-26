import BuildVars._

// keep this to the same level as sanamo otherwise we will evict the
// only version of the library that scanamo will work with
lazy val AwsSdkVersion = "1.11.8"

name := "atom-manager-play"

// Necessary because of a conflict between catz, imported by scanamo 1.0.0M9, and scanamo-scrooge
dependencyOverrides += "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4"

libraryDependencies ++= Seq(
  "com.typesafe.play"      %% "play"                  % playVersion,
  "com.gu"                 %% "content-atom-model"    % contentAtomVersion,
  "org.scalatestplus.play" %% "scalatestplus-play"    % "5.1.0"   % "test",
  "com.amazonaws"          %  "aws-java-sdk-dynamodb" % awsVersion,
  "org.scalatestplus" %% "mockito-4-5" % "3.2.12.0" % "test",
  "com.typesafe.play"      %% "play-test"             % "2.6.0" % "test",
  guice
)
