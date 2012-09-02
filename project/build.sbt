import Defaults.sbtPluginExtra

resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.5")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0-RC1")

resolvers += Resolver.url("sbt-plugin-releases", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

libraryDependencies += sbtPluginExtra("com.jsuereth" % "xsbt-gpg-plugin" % "0.6.1", "0.11.3", "2.9.1")
