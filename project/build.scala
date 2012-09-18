import sbt._

object Builds extends sbt.Build {
  import Keys._
  import sbtrelease.ReleasePlugin._
  import sbtbuildinfo.Plugin._
  import ls.Plugin.{lsSettings,LsKeys}

  lazy val buildSettings = Defaults.defaultSettings ++ releaseSettings ++ Seq( 
    organization := "net.rosien",
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.9.0-1", "2.9.1"),
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    publishArtifact in Test := false,
    publishMavenStyle := true,
    publishTo <<= version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := (
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
      </developers>)
  )

  // Depends on 'core' so that one can depend on 'sniff' or 'sniff-core'.
  lazy val root = Project("sniff", file("."),
    settings = buildSettings ++ lsSettings ++ Seq(
      name := "sniff",
      description := "Keep your code fresh smelling: generate bad code smells specs2 specifications for any source language",
      LsKeys.tags in LsKeys.lsync := Seq("test", "specs2", "code smells")
    )) aggregate(app, core) dependsOn(core)

  lazy val app = Project("sniff-app", file("app"),
    settings = buildSettings ++ conscript.Harness.conscriptSettings ++ Seq(
      description := "Command line tool to sniff source code"
    )) dependsOn(core)

  lazy val core = Project("sniff-core", file("core"),
    settings = buildSettings ++ buildInfoSettings ++ Seq(
      description := "sniff: the inner nose",
      sourceGenerators in Compile <+= buildInfo,
      buildInfoPackage := "net.rosien.sniff",
      libraryDependencies ++= Seq(
        "org.specs2" %% "specs2" % "1.8.2",
        "org.scalaz" %% "scalaz-core" % "6.0.3"
      )
    )) 
}
