organization in ThisBuild := "com.gxhr"
version in ThisBuild := "0.1.2"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.8"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.0"
val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.710"

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
      scalaTest,
      lagomScaladslAkkaDiscovery,
      akkaDiscoveryKubernetesApi,
      awsS3
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`miniapp-api`)
  .settings(dockerExposedPorts := Seq(9000, 9001))

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

// kubernetes [dev,test,prod]
//lagomCassandraEnabled in ThisBuild := false
// this works only in dev k8s
//lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "tcp://miniapp-cassandra-nodeport-service:9042")
//lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "tcp://192.168.64.5:31043")
lagomKafkaEnabled in ThisBuild := false
// this works only in dev local
// everything cassandra should be commented to make it work