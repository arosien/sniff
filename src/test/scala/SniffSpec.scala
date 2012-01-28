package net.rosien.sniff

import org.specs2.Specification
import org.specs2.reporter.Notifier
import org.specs2.runner.ClassRunner
import org.specs2.reporter.Reporter
import org.specs2.reporter.NotifierReporter
import org.specs2.execute.Details
import org.specs2.matcher.MustThrownMatchers

class SniffSpec extends Specification with MustThrownMatchers {
  import net.rosien.sniff._
  
  def is = 
      "Scala snippets should pass"               ! snippets().allPass ^
      "Snippets without ignores fails for NoURL" ! snippets().oneFailsWithoutIgnores ^
      "FilesNamed scans specific file"           ! snippets().filesNamed
  
  val scalaSnippets = CodeSnippets(Scala,
      Smell('NoURL, """java\.net\.URL""".r, rationale = "URL actually resolves hostnames over the network, use java.net.URI instead"),
      Smell('NoSystemProperties, """System\.\w+Property\(""".r, rationale = "Use Scala-idomatic sys.props object"),
      Smell('NoJUnit, """org\.junit""".r, rationale = "Use org.specs2"),
      Smell('UseSpecs2, """\s*import\s+org\.specs\.""".r, rationale = "Use org.specs2 (not v1)"),
      Smell('NoSysOut, """\bSystem\.out\.print""".r, rationale = "Use a logger"),
      Smell('NoSysErr, """\bSystem\.err\.print""".r, rationale = "Use a logger"))

  val logback = CodeSnippets(FilesNamed("logback.xml"),
      Smell('NoMethodNameLogging, """%M""".r, "%M is an expensive logging option"))

  // import java.net.URL <-- should be ignored from the clause below:
  implicit val ignore = Ignores(
      Ignore('NoURL, "src/test/scala/SniffSpec.scala"))

  def scalaSpec(implicit ignore: Ignores) = new Specification {
    val is = "Code shouldn't smell" ^ scalaSnippets.sniff("src/main/scala", "src/test/scala")
  }
  
  def logbackSpec(implicit ignore: Ignores) = new Specification {
    val is = "Code shouldn't smell" ^ logback.sniff("src/main/resources")
  }

  // TODO: I'm sure there's a better way to do this.
  class CollectingSpecRunner extends ClassRunner with Notifier {
    override lazy val reporter: Reporter = new NotifierReporter {
      val notifier = CollectingSpecRunner.this
    }
    
    import scala.collection.mutable
    val successes = mutable.Set[String]()
    val fails     = mutable.Set[String]()

    def specStart(title: String, location: String) {}
    def specEnd(title: String, location: String) {}
    def contextStart(text: String, location: String) {}
    def contextEnd(text: String, location: String) {}
    def text(text: String, location: String) {}
    def exampleStarted(name: String, location: String) {}
    def exampleSuccess(name: String, duration: Long) = successes += name
    def exampleFailure(name: String, message: String, location: String, f: Throwable, details: Details, duration: Long) = fails += name
    def exampleError  (name: String, message: String, location: String, f: Throwable, duration: Long) {}
    def exampleSkipped(name: String, message: String, duration: Long) {}
    def examplePending(name: String, message: String, duration: Long) {}
  }

  case class snippets() {
    val runner = new CollectingSpecRunner
    
    def allPass = {
      runner(scalaSpec) must beRight
      // N snippets * 2 files - 1 ignore
      runner.successes must have size(scalaSnippets.snippets.size * 2 - 1)
      runner.fails must beEmpty
    }
    
    def oneFailsWithoutIgnores = {
      runner(scalaSpec(Ignores())) must beRight
      // N snippets * 2 file - 1 fail
      runner.successes must have size(scalaSnippets.snippets.size * 2 - 1)
      runner.fails must have size(1)
      runner.fails must containMatch("NoURL") and have containMatch("src/test/scala/SniffSpec.scala")
    }
    
    def filesNamed = {
      runner(logbackSpec) must beRight
      // 1 snippet * 1 file
      runner.successes must beEmpty
      runner.fails must have size(1)
      runner.fails must containMatch("NoMethodNameLogging") and have containMatch("src/main/resources/logback.xml")
    }
    
  }
}