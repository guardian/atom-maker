import BuildVars._

name := "atom-manager-play"

libraryDependencies ++= Seq(
  "com.typesafe.play"      %% "play"                  % playVersion,
  "com.gu"                 %% "content-atom-model"    % contentAtomVersion,
  "org.scalatestplus.play" %% "scalatestplus-play"    % "6.0.1" % Test,
  "com.amazonaws"          %  "aws-java-sdk-dynamodb" % awsVersion,
  "org.mockito"            %  "mockito-core"          % mockitoVersion % Test,
  "com.typesafe.play"      %% "play-test"             % playVersion % Test,
  guice
)
