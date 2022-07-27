import sbt._

object BuildVars {
  lazy val awsVersion         = "1.11.8"
  lazy val contentAtomVersion = "3.2.0"
  lazy val scroogeVersion     = "22.4.0"
  lazy val akkaVersion        = "2.5.3"
  lazy val playVersion        = "2.6.0"

  lazy val scanamoDeps = Seq(
    "org.scanamo" %% "scanamo" % "1.0.0-M9",
    "com.gu" %% "scanamo-scrooge" % "0.2.2-SNAPSHOT"
  )
}
