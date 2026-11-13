import BuildVars._

name := "atom-manager-play"

val jacksonVersion = "2.18.11"

dependencyOverrides ++= Seq(
  "ch.qos.logback" % "logback-core" % "1.5.34",
  "ch.qos.logback" % "logback-classic" % "1.5.34"
)

libraryDependencies ++= Seq(
  "at.yawk.lz4" % "lz4-java" % "1.11.4",
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

excludeDependencies += ExclusionRule("org.lz4", "lz4-java")

// Tests use PlaySpec and FakeRequest, not browser helpers. Keep play-test itself.
excludeDependencies ++= Seq(
  ExclusionRule(organization = "io.fluentlenium"),
  ExclusionRule(organization = "org.seleniumhq.selenium"),
  ExclusionRule("org.scalatestplus", "selenium-4-12_2.13")
)
