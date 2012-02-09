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

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra :=
  <url>https://github.com/arosien/sniff</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:arosien/sniff.git</url>
    <connection>scm:git:git@github.com:arosien/sniff.git</connection>
  </scm>
  <developers>
    <developer>
      <id>arosien</id>
      <name>Adam Rosien</name>
      <url>http://rosien.net</url>
    </developer>
  </developers>
