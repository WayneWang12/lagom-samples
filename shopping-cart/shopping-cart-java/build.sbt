import com.lightbend.lagom.core.LagomVersion
import play.core.PlayVersion.{ current => playVersion }

organization in ThisBuild := "com.example"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.1"

// Update the version generated by sbt-dynver to remove any + characters, since these are illegal in docker tags
version in ThisBuild ~= (_.replace('+', '-'))
dynver in ThisBuild ~= (_.replace('+', '-'))

val hibernateEntityManager = "org.hibernate" % "hibernate-entitymanager" % "5.4.2.Final"
val jpaApi                 = "org.hibernate.javax.persistence" % "hibernate-jpa-2.1-api" % "1.0.0.Final"
val validationApi          = "javax.validation" % "validation-api" % "1.1.0.Final"

val akkaVersion          = "2.6.0-M8"
val akkaDiscovery        = "com.typesafe.akka" %% "akka-discovery"         % akkaVersion
val akkaProtobuf         = "com.typesafe.akka" %% "akka-protobuf"          % akkaVersion
val akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
val akkaTestkit          = "com.typesafe.akka" %% "akka-stream-testkit"    % akkaVersion

val playJavaClusterSharding = "com.typesafe.play" %% "play-java-cluster-sharding" % playVersion

lazy val `shopping-cart-java` = (project in file("."))
  .aggregate(`shopping-cart-api`, `shopping-cart`, `inventory-api`, inventory)

lazy val `shopping-cart-api` = (project in file("shopping-cart-api"))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi,
      lombok
    )
  )

lazy val `shopping-cart` = (project in file("shopping-cart"))
  .enablePlugins(LagomJava)
  .settings(common)
  .settings(dockerSettings)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslPersistenceJdbc,
      lagomJavadslPersistenceJpa,
      lagomJavadslKafkaBroker,
      lagomLogback,
      lagomJavadslTestKit,
      lombok,
      postgresDriver,
      hamcrestLibrary,
      lagomJavadslAkkaDiscovery,
      akkaDiscoveryKubernetesApi,
      akkaDiscovery,
      akkaProtobuf,
      akkaPersistenceQuery,
      hibernateEntityManager,
      jpaApi,
      validationApi
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`shopping-cart-api`)

lazy val `inventory-api` = (project in file("inventory-api"))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi
    )
  )

lazy val inventory = (project in file("inventory"))
  .enablePlugins(LagomJava)
  .settings(common)
  .settings(dockerSettings)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslKafkaClient,
      lagomLogback,
      lagomJavadslTestKit,
      lagomJavadslAkkaDiscovery
    )
  )
  .dependsOn(`shopping-cart-api`, `inventory-api`)

val lombok = "org.projectlombok" % "lombok" % "1.18.8"
val postgresDriver = "org.postgresql" % "postgresql" % "42.2.8"
val hamcrestLibrary = "org.hamcrest" % "hamcrest-library" % "2.1" % Test

val akkaManagementVersion = "1.0.1"
val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion
val lagomJavadslAkkaDiscovery = "com.lightbend.lagom" %% "lagom-javadsl-akka-discovery-service-locator" % LagomVersion.current

def common = Seq(
  javacOptions in Compile := Seq("-g", "-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation", "-parameters", "-Werror")
)

def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := getDockerBaseImage(),
  dockerUsername := sys.props.get("docker.username"),
  dockerRepository := sys.props.get("docker.registry")
)

def getDockerBaseImage(): String = sys.props.get("java.version") match {
  case Some(v) if v.startsWith("11") => "adoptopenjdk/openjdk11"
  case _ => "adoptopenjdk/openjdk8"
}

lagomCassandraEnabled in ThisBuild := false
