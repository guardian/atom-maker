import sbt._

object BuildVars {
  lazy val awsVersion         = "1.11.8"
  lazy val contentAtomVersion = "3.2.0"
  lazy val scroogeVersion     = "19.9.0"
  lazy val akkaVersion        = "2.5.3"
  lazy val playVersion        = "2.7.9"
  lazy val mockitoVersion     = "2.0.97-beta"

  lazy val scanamoDeps = Seq(
    "org.scanamo" %% "scanamo" % "1.0.0-M9"
  )
}
