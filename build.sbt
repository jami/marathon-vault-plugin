organization := "comptelfwd"

name := "marathon-vault-plugin"

version := "0.1.0"

scalaVersion := "2.11.11"

resolvers ++= Seq(
  "Mesosphere Public Repo" at "http://downloads.mesosphere.io/maven",
  Resolver.jcenterRepo
)

libraryDependencies ++= Seq(
  "mesosphere.marathon" %% "plugin-interface" % "1.5.1" % "provided",
  "mesosphere.marathon" %% "marathon" % "1.5.1" % "provided",
  "org.slf4j" % "slf4j-api" % "1.7.21" % "provided",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
//  "org.mockito" % "mockito-core" % "2.7.22" % "test"

)
