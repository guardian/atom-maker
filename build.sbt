scalaVersion in ThisBuild := "2.11.8"

name := "atom-maker-lib"

organization in ThisBuild := "com.gu"

/*libraryDependencies ++= Seq(
  "com.gu"                     %% "content-atom-model"           % contentAtomVersion,
  "org.apache.thrift"          %  "libthrift"                    % "0.9.3",
  "com.twitter"                %% "scrooge-core"                 % scroogeVersion,
  "com.twitter"                %% "scrooge-serializer"           % scroogeVersion,
  "com.amazonaws"              % "aws-java-sdk-sts"              % awsVersion,
  "com.typesafe.scala-logging" %% "scala-logging"                % "3.4.0",
  "org.typelevel"              %% "cats-core"                    % "0.6.0", // for interacting with scanamo
  "com.fasterxml.jackson.core" %  "jackson-databind"             % "2.7.0",
  "com.gu"                     %% "pan-domain-auth-play_2-5"     % pandaVer,
  ws, // for panda
  "com.gu"                     %% "pan-domain-auth-verification" % pandaVer,
  "com.gu"                     %% "pan-domain-auth-core"         % pandaVer,
  "org.scalatestplus.play"     %% "scalatestplus-play"           % "1.5.0"   % "test",
  "org.mockito"                %  "mockito-core"                 % mockitoVersion % "test",
  "org.scala-lang.modules"     %% "scala-xml"                    % "1.0.5"   % "test"
) ++ scanamoDeps*/

lazy val atomPublisher = project in file("./atom-publisher-lib")

lazy val atomManagerPlay = (project in file("./atom-manager-play-lib"))
  .settings(publishArtifact in Test := true)
  .dependsOn(atomPublisher % "test->test;compile->compile")

lazy val root = (project in file("."))
  .dependsOn(atomPublisher, atomManagerPlay % "test->test;compile->compile")
  .aggregate(atomPublisher, atomManagerPlay)
  .settings(aggregate := false, aggregate in Test := true)
