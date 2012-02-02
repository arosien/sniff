package net.rosien.sniff

import org.specs2.Specification
import org.specs2.reporter.Notifier
import org.specs2.runner.ClassRunner
import org.specs2.reporter.Reporter
import org.specs2.reporter.NotifierReporter
import org.specs2.execute.Details
import org.specs2.matcher.MustThrownMatchers
import java.io.File

class SniffSpec extends Specification with MustThrownMatchers {
  def is = 
      "Scala Language converts to snippets"      ! snippets().langConvertsToSnippets ^
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
    val logback = CodeSnippets(FilesNamed("logback.xml"),
        Smell('NoMethodNameLogging, """%M""".r, "%M is an expensive logging option"))

    def spec(snippets: CodeSnippets, dirs: Path*)(implicit ignores: Ignores) = new Specification {
      val is = "Code shouldn't smell" ^ snippets.sniff(dirs: _*)
    }
  
    val runner = new CollectingSpecRunner
    val numFiles = getFileTree(new File("src/main/scala")).filter(Scala).size + getFileTree(new File("src/test/scala")).filter(Scala).size
    
    def langConvertsToSnippets = Scala.snippets.smells must not be empty
    
    def allPass = {
      // import java.net.URL <-- should be ignored from the clause below:
      implicit val ignores = Ignores(Ignore('NoURL, "src/test/scala/SniffSpec.scala"))
      runner(spec(Scala.snippets, "src/main/scala", "src/test/scala")) must beRight
      runner.fails must beEmpty
      runner.successes must have size(Scala.snippets.smells.size * numFiles - 1)
    }
    
    def oneFailsWithoutIgnores = {
      implicit val ignores = Ignores()
      runner(spec(Scala.snippets, "src/main/scala", "src/test/scala")) must beRight
      runner.fails must have size(1)
      runner.fails must containMatch("NoURL") and have containMatch("src/test/scala/SniffSpec.scala")
      runner.successes must have size(Scala.snippets.smells.size * numFiles - 1)
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