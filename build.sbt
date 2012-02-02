organization := "net.rosien"

name := "sniff"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

crossScalaVersions := Seq("2.9.0-1", "2.9.1")

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.7.1",
  "org.scalaz" %% "scalaz-core" % "6.0.3"
)

scalacOptions ++= Seq("-deprecation", "-unchecked")

pomExtra :=
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
