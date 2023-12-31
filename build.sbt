ThisBuild / scalaVersion := "2.12.11"

val AkkaVersion = "2.6.17"
val AkkaManagementVersion = "1.0.9"

resolvers += ("custome1" at "http://4thline.org/m2").withAllowInsecureProtocol(true)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
"com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
"com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
 "org.fourthline.cling" % "cling-core" % "2.1.2",
 "org.fourthline.cling" % "cling-support" % "2.1.2",
"com.typesafe" % "config" % "1.4.1",
 "org.scalafx" %% "scalafx" % "8.0.192-R14",
  "org.scalafx" %% "scalafxml-core-sfx8" % "0.5",
"com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
"com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
"com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
  "org.openjfx" % "javafx-fxml" % "11",
  "org.scalikejdbc" %% "scalikejdbc"       % "3.1.0",
  "com.h2database"  %  "h2"                % "1.4.196",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "ch.qos.logback" % "logback-access" % "1.2.3",
  "org.apache.derby" % "derby" % "10.12.1.1"
)

fork := false