import Defaults.sbtPluginExtra

resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.5")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0-RC1")

resolvers += Resolver.url("sbt-plugin-releases", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

libraryDependencies += sbtPluginExtra("com.jsuereth" % "xsbt-gpg-plugin" % "0.6.1", "0.11.3", "2.9.1")

addSbtPlugin("net.databinder" % "conscript-plugin" % "0.3.4")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.1.2")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")
