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
  "org.scalatestplus.play" %% "scalatestplus-play"    % "3.1.3"   % "test",
  "com.amazonaws"          %  "aws-java-sdk-dynamodb" % awsVersion,
  "org.scalatestplus"      %% "mockito-3-3"           % "3.1.2.0" % "test",
  "com.typesafe.play"      %% "play-test"             % "2.6.0" % "test",
  guice
)


dependencyOverrides ++= Seq(
  "org.mockito"                %  "mockito-core"         % "2.7.22",
  "org.scalatest"              %% "scalatest"            % "3.0.8"
)
