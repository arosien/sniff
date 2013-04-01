package net.rosien.sniff

import org.specs2.Specification
import org.specs2.scalaz.ValidationMatchers
import java.io.File
import Language._
import scalaz._
import Scalaz._

object SpecIgnores {
  implicit val ignores = Ignores(
      Ignore('NoURL,                      """SniffSpec.scala$""".r),
      Ignore('NoJavaDeprecatedAnnotation, "src/main/scala/smells.scala"))
}

class MetaSpec extends Specification with ValidationMatchers {
  import SpecIgnores._

  def is = "Sniff should not smell" ! fresh

  def fresh = {
    val sniffs = Scala.snippets.sniff("core/src/main/scala", "core/src/test/scala").map(_.sniff.liftFailNel)

    (sniffs must not beEmpty) and
    (sniffs.sequence[`ValidationNEL[Stink]`, FileSmell] must beSuccessful)
  }
}
/*
class ReadmeSpec extends Specification {
  def is = "README examples compile" ^
      "usage"     ! usage ^
      "ignores"   ! ignores ^
      "my smells" ! mySmells ^
      end

  def usage = {
    val spec = "Scala code shouldn't smell" ^ Scala.snippets.sniff("src/main/scala", "src/test/scala")
    success
  }

  def ignores = {
    implicit val ignore = Ignores(
      Ignore('NoURL, "src/test/scala/SniffSpec.scala"))
    success
  }

  def mySmells = {
    val mySmells =
        Smell('NoMutableCollections, """scala\.collection\.mutable""".r, rationale = "Immutable is better than mutable. - El Jefe", Scala, 'movieReferences) ::
        // more smells
        Nil
    class SniffSpec extends Specification {
      def is = "Die smells die" ^ CodeSnippets(Scala, mySmells: _*).sniff("src/main/scala", "src/test/scala")
    }
    success
  }
}

class SniffSpec extends Specification with MustThrownMatchers {
  def is = "Sniff should" ^
      "Scala Language converts to snippets"      ! snippets().langConvertsToSnippets ^
      "Scala snippets should pass"               ! snippets().allPass ^
      "Snippets without ignores fails for NoURL" ! snippets().oneFailsWithoutIgnores ^
      "FilesNamed scans specific file"           ! snippets().filesNamed ^
      end

  case class snippets() {
    def spec(snippets: CodeSnippets, dirs: Path*)(implicit ignores: Ignores) = new Specification {
      val is = "Code shouldn't smell" ^ snippets.sniff(dirs: _*)
    }

    val numFiles = getFileTree(new File("core/src/main/scala")).filter(Scala.fileFilter).size +
        getFileTree(new File("core/src/test/scala")).filter(Scala.fileFilter).size

    def langConvertsToSnippets = Scala.snippets.smells must not be empty

    def allPass = {
      // import java.net.URL <-- should be ignored from the clause below:
      import SpecIgnores._
      runner(spec(Scala.snippets, "core/src/main/scala", "core/src/test/scala"))
      runner.fails must_== 0
      runner.skipped must_== ignores.ignores.size
      runner.successes must_== Scala.snippets.smells.size * numFiles - runner.skipped
    }

    def oneFailsWithoutIgnores = {
      runner(spec(Scala.snippets, "core/src/main/scala", "core/src/test/scala")(Ignores()))
      runner.fails must_== SpecIgnores.ignores.ignores.size
      runner.skipped must_== 0
      runner.successes must_== Scala.snippets.smells.size * numFiles - runner.fails
    }

    def filesNamed = {
      val logback = CodeSnippets(FilesNamed("logback.xml"),
        Smell('NoMethodNameLogging, """%M""".r, "%M is an expensive logging option"))
      runner(spec(logback, "core/src/test/resources")(Ignores()))
      runner.successes must_== 0
      runner.fails must_== 1
      runner.skipped must_== 0
    }

  }
}
*/