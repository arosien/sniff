package net.rosien.sniff

import org.specs2.Specification
import org.specs2.reporter.Notifier
import org.specs2.runner.ClassRunner
import org.specs2.reporter.Reporter
import org.specs2.reporter.NotifierReporter
import org.specs2.execute.Details
import org.specs2.matcher.MustThrownMatchers

class SniffSpec extends Specification with MustThrownMatchers {
  def is = 
      "Scala snippets should pass"               ! snippets().allPass ^
      "Snippets without ignores fails for NoURL" ! snippets().oneFailsWithoutIgnores ^
      "FilesNamed scans specific file"           ! snippets().filesNamed
  
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
    val scalaSnippets = CodeSnippets(Scala,
        Smell('NoURL, """java\.net\.URL""".r, rationale = "URL actually resolves hostnames over the network, use java.net.URI instead"))

    val logback = CodeSnippets(FilesNamed("logback.xml"),
        Smell('NoMethodNameLogging, """%M""".r, "%M is an expensive logging option"))

    def spec(snippets: CodeSnippets, dirs: Path*)(implicit ignores: Ignores) = new Specification {
      val is = "Code shouldn't smell" ^ snippets.sniff(dirs: _*)
    }
  
    val runner = new CollectingSpecRunner
    val numFiles = 2
    
    def allPass = {
      // import java.net.URL <-- should be ignored from the clause below:
      implicit val ignores = Ignores(Ignore('NoURL, "src/test/scala/SniffSpec.scala"))
      runner(spec(scalaSnippets, "src/main/scala", "src/test/scala")) must beRight
      runner.successes must have size(scalaSnippets.snippets.size * numFiles - 1)
      runner.fails must beEmpty
    }
    
    def oneFailsWithoutIgnores = {
      implicit val ignores = Ignores()
      runner(spec(scalaSnippets, "src/main/scala", "src/test/scala")) must beRight
      runner.successes must have size(scalaSnippets.snippets.size * numFiles - 1)
      runner.fails must have size(1)
      runner.fails must containMatch("NoURL") and have containMatch("src/test/scala/SniffSpec.scala")
    }
    
    def filesNamed = {
      implicit val ignores = Ignores()
      runner(spec(logback, "src/test/resources")) must beRight
      runner.successes must beEmpty
      runner.fails must have size(1)
      runner.fails must containMatch("NoMethodNameLogging") and have containMatch("src/test/resources/logback.xml")
    }
    
  }
}