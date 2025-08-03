val scala3Version = "3.7.2"
val playWsStandaloneVersion = "3.0.7"

lazy val root = project
  .in(file("."))
  .settings(
    name := "grid-api-tests",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.playframework" %% "play-ahc-ws-standalone" % playWsStandaloneVersion,
    libraryDependencies += "org.playframework" %% "play-ws-standalone-json" % playWsStandaloneVersion,

    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )
