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
  val ignores = Ignores(Ignore('NoURL, "src/test/scala/SniffSpec.scala"))
  
  def is = "Sniff should" ^
      "not have any Scala smells itself" ^ 
        Scala.snippets.sniff("src/main/scala", "src/test/scala")(ignores) ^
        end ^
      "and" ^
        "Scala Language converts to snippets"      ! snippets().langConvertsToSnippets ^
        "Scala snippets should pass"               ! snippets().allPass ^
        "Snippets without ignores fails for NoURL" ! snippets().oneFailsWithoutIgnores ^
        "FilesNamed scans specific file"           ! snippets().filesNamed ^
        end
  
  // TODO: I'm sure there's a better way to do this.
  class CollectingSpecRunner extends ClassRunner with Notifier {
    override lazy val reporter: Reporter = new NotifierReporter {
      val notifier = CollectingSpecRunner.this
    }
    
    import scala.collection.mutable
    var successes = 0
    var fails     = 0
    var skipped   = 0

    def specStart(title: String, location: String) {}
    def specEnd(title: String, location: String) {}
    def contextStart(text: String, location: String) {}
    def contextEnd(text: String, location: String) {}
    def text(text: String, location: String) {}
    def exampleStarted(name: String, location: String) {}
    def exampleSuccess(name: String, duration: Long) = successes += 1
    def exampleFailure(name: String, message: String, location: String, f: Throwable, details: Details, duration: Long) = fails += 1
    def exampleError  (name: String, message: String, location: String, f: Throwable, duration: Long) {}
    def exampleSkipped(name: String, message: String, duration: Long) = skipped += 1
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
      implicit val ignores = SniffSpec.this.ignores
      runner(spec(Scala.snippets, "src/main/scala", "src/test/scala")) must beRight
      runner.fails must_== 0
      runner.skipped must_== 1
      runner.successes must_== Scala.snippets.smells.size * numFiles - 1
    }
    
    def oneFailsWithoutIgnores = {
      implicit val ignores = Ignores() // Don't ignore URL in comment above
      runner(spec(Scala.snippets, "src/main/scala", "src/test/scala")) must beRight
      runner.fails must_== 1
      runner.skipped must_== 0
      runner.successes must_== Scala.snippets.smells.size * numFiles - 1
    }
    
    def filesNamed = {
      implicit val ignores = Ignores()
      runner(spec(logback, "src/test/resources")) must beRight
      runner.successes must_== 0
      runner.fails must_== 1
      runner.skipped must_== 0
    }
    
  }
}