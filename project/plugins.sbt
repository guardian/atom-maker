// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.9")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.15")

// for creating test cases that use a local dynamodb
addSbtPlugin("com.localytics" % "sbt-dynamodb" % "2.0.3")

/*
   scala-xml has been updated to 2.x in sbt, but not in other sbt plugins like sbt-native-packager
   See: https://github.com/scala/bug/issues/12632
   This is effectively overrides the safeguards (early-semver) put in place by the library authors ensuring binary compatibility.
   We consider this a safe operation because when set under `projects/` (ie *not* in `build.sbt` itself) it only affects the
   compilation of build.sbt, not of the application build itself.
   Once the build has succeeded, there is no further risk (ie of a runtime exception due to clashing versions of `scala-xml`).
 */
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
