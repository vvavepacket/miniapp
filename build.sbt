organization in ThisBuild := "com.gxhr"
version in ThisBuild := "0.1.0"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.8"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `miniapp` = (project in file("."))
  .aggregate(`miniapp-api`, `miniapp-impl`, `miniapp-stream-api`, `miniapp-stream-impl`)

lazy val `miniapp-api` = (project in file("miniapp-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `miniapp-impl` = (project in file("miniapp-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`miniapp-api`)

lazy val `miniapp-stream-api` = (project in file("miniapp-stream-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `miniapp-stream-impl` = (project in file("miniapp-stream-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`miniapp-stream-api`, `miniapp-api`)
