import BuildVars._

name := "atom-manager-play"

libraryDependencies ++= Seq(
  "org.playframework"      %% "play"                  % playVersion,
  "com.gu"                 %% "content-atom-model"    % contentAtomVersion,
  "org.scalatestplus.play" %% "scalatestplus-play"    % "7.0.2" % Test,
  "com.amazonaws"          %  "aws-java-sdk-dynamodb" % awsVersion,
  "org.mockito"            %  "mockito-core"          % mockitoVersion % Test,
  "org.playframework"      %% "play-test"             % playVersion % Test
)
