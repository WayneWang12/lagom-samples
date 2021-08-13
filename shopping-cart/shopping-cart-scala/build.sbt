import com.lightbend.lagom.core.LagomVersion.{ current => lagomVersion }

organization in ThisBuild := "com.example"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.5"

val postgresDriver             = "org.postgresql"                % "postgresql"                                    % "42.2.18"
val macwire                    = "com.softwaremill.macwire"     %% "macros"                                        % "2.3.7" % "provided"
val scalaTest                  = "org.scalatest"                %% "scalatest"                                     % "3.2.2" % Test
val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api"                 % "1.0.10"
val lagomScaladslAkkaDiscovery = "com.lightbend.lagom"          %% "lagom-scaladsl-akka-discovery-service-locator" % lagomVersion

ThisBuild / scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings")

def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := getDockerBaseImage(),
  dockerUsername := sys.props.get("docker.username"),
  dockerRepository := sys.props.get("docker.registry")
)

def getDockerBaseImage(): String = sys.props.get("java.version") match {
  case Some(v) if v.startsWith("11") => "adoptopenjdk/openjdk11"
  case _                             => "adoptopenjdk/openjdk8"
}

// Update the version generated by sbt-dynver to remove any + characters, since these are illegal in docker tags
version in ThisBuild ~= (_.replace('+', '-'))
dynver in ThisBuild ~= (_.replace('+', '-'))

lazy val `shopping-cart-scala` = (project in file("."))
  .aggregate(`shopping-cart-api`, `shopping-cart`, `inventory-api`, inventory)

lazy val `shopping-cart-api` = (project in file("shopping-cart-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `shopping-cart` = (project in file("shopping-cart"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      postgresDriver,
      lagomScaladslAkkaDiscovery,
      akkaDiscoveryKubernetesApi,
      "com.typesafe.akka" %% "akka-persistence-testkit" % "2.6.8" % Test,
      "com.h2database"     % "h2"                       % "1.4.200"
    )
  )
  .settings(dockerSettings)
  .settings(lagomForkedTestSettings)
  .dependsOn(`shopping-cart-api`)

lazy val `inventory-api` = (project in file("inventory-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val inventory = (project in file("inventory"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslKafkaClient,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      lagomScaladslAkkaDiscovery
    )
  )
  .settings(dockerSettings)
  .dependsOn(`inventory-api`, `shopping-cart-api`)

// The project uses PostgreSQL
lagomCassandraEnabled in ThisBuild := false

// Use Kafka server running in a docker container
lagomKafkaEnabled in ThisBuild := false
lagomKafkaPort in ThisBuild := 9092
