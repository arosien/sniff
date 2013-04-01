import sbt._

object Builds extends sbt.Build {
  import Keys._
  import sbtrelease.ReleasePlugin._
  import sbtbuildinfo.Plugin._
  import ls.Plugin.{lsSettings,LsKeys}

  lazy val buildSettings = Defaults.defaultSettings ++ releaseSettings ++ Seq(
    organization := "net.rosien",
    scalaVersion := "2.9.2",
    crossScalaVersions := Seq("2.9.2", "2.10.1"),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings"),
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
      description := "Keep your code fresh smelling: generate bad code smell detectors for any source language",
      LsKeys.tags in LsKeys.lsync := Seq("test", "code smells")
    )) aggregate(app, core) dependsOn(core)

  lazy val app = Project("sniff-app", file("app"),
    settings = buildSettings ++ conscript.Harness.conscriptSettings ++ Seq(
      description := "Command line tool to sniff source code",
      libraryDependencies += "org.rogach" %% "scallop" % "0.8.0"
    )) dependsOn(core)

  lazy val core = Project("sniff-core", file("core"),
    settings = buildSettings ++ buildInfoSettings ++ Seq(
      description := "sniff: the inner nose",
      sourceGenerators in Compile <+= buildInfo,
      buildInfoPackage := "net.rosien.sniff",
      libraryDependencies ++= Seq(
        "org.scalaz"    %% "scalaz-core"    % "6.0.4",
        "org.specs2"    %% "specs2"         % "1.12.3" % "test",
        "org.typelevel" %% "scalaz6-specs2" % "0.1"    % "test"
      )
    ))
}
