import BuildVars._

// keep this to the same level as sanamo otherwise we will evict the
// only version of the library that scanamo will work with
lazy val AwsSdkVersion = "1.11.8"

name := "atom-manager-play"

libraryDependencies ++= Seq(
  "com.typesafe.play"      %% "play"                  % playVersion,
  "com.gu"                 %% "content-atom-model"    % contentAtomVersion,
  "org.scalatestplus.play" %% "scalatestplus-play"    % "1.5.0"   % "test",
  "com.amazonaws"          %  "aws-java-sdk-dynamodb" % awsVersion,
  "org.mockito"            %  "mockito-core"          % mockitoVersion % "test",
  "com.typesafe.play"      %% "play-test"             % "2.6.0" % "test",
  guice
)
