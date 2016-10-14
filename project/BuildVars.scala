import sbt._

object BuildVars {
  lazy val awsVersion         = "1.10.69"
  lazy val contentAtomVersion = "2.4.6"
  lazy val scroogeVersion     = "4.2.0"
  lazy val akkaVersion        = "2.4.8"
  lazy val playVersion        = "2.5.3"
  lazy val mockitoVersion     = "2.0.97-beta"

  lazy val scanamoDeps = Seq(
    "com.gu"                     % "scanamo_2.11"          % "0.7.0",
    "com.gu"                     % "scanamo-scrooge_2.11"  % "0.1.2"
  )
}
