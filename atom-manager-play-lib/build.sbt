import BuildVars._

name := "atom-manager-play"

val jacksonVersion = "2.18.11"

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "33.7.2-jre",
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "org.playframework"      %% "play"                  % playVersion,
  "com.gu"                 %% "content-atom-model"    % contentAtomVersion,
  "org.scalatestplus.play" %% "scalatestplus-play"    % "7.0.1" % Test,
  "org.mockito"            %  "mockito-core"          % mockitoVersion % Test,
  "org.playframework"      %% "play-test"             % playVersion % Test
)
