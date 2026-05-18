ThisBuild / scalaVersion := "3.8.3"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "scraper",
    libraryDependencies ++= Seq(
      "dev.zio"  %% "zio"          % "2.1.14",
      "dev.zio"  %% "zio-http"     % "3.0.1",
      "org.jsoup" % "jsoup"        % "1.18.1",
      "dev.zio"  %% "zio-test"     % "2.1.14" % Test,
      "dev.zio"  %% "zio-test-sbt" % "2.1.14" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
  )
