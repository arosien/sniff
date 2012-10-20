resolvers ++= Seq(
  "less is" at "http://repo.lessis.me"
)

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.6")

addSbtPlugin("net.databinder" % "conscript-plugin" % "0.3.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.0")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")
