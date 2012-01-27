package net.rosien.sniff

import org.specs2.Specification

class SniffSpec extends Specification { 
  import net.rosien.sniff._
  
  val snippets = CodeSnippets(Scala,
      Smell('NoURL, """java\.net\.URL""".r, rationale = "URL actually resolves hostnames over the network, use java.net.URI instead"),
      Smell('NoSystemProperties, """System\.\w+Property\(""".r, rationale = "Use Scala-idomatic sys.props object"),
      Smell('NoJUnit, """org\.junit""".r, rationale = "Use org.specs2"),
      Smell('UseSpecs2, """\s*import\s+org\.specs\.""".r, rationale = "Use org.specs2 (not v1)"),
      Smell('NoSysOut, """\bSystem\.out\.print""".r, rationale = "Use a logger"),
      Smell('NoSysErr, """\bSystem\.err\.print""".r, rationale = "Use a logger"))
  
  // import java.net.URL shouldn't be found because of the ignore clause below:
  implicit val ignore = Ignores(
      Ignore('NoURL, "src/test/scala/SniffSpec.scala"))
      
  val is = "Code shouldn't smell" ^ snippets.sniff("src/main/scala", "src/test/scala")
}