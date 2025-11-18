addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.2.0")

// for creating test cases that use a local dynamodb
addSbtPlugin("com.localytics" % "sbt-dynamodb" % "2.0.3")

addDependencyTreePlugin

/*
   scala-xml has been updated to 2.x in sbt, but not in other sbt plugins like sbt-native-packager
   See: https://github.com/scala/bug/issues/12632
   This is effectively overrides the safeguards (early-semver) put in place by the library authors ensuring binary compatibility.
   We consider this a safe operation because when set under `projects/` (ie *not* in `build.sbt` itself) it only affects the
   compilation of build.sbt, not of the application build itself.
   Once the build has succeeded, there is no further risk (ie of a runtime exception due to clashing versions of `scala-xml`).
 */
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
