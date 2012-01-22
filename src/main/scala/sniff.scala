package net.rosien

import org.specs2.matcher._
import scala.util.matching.Regex
import scalaz._
import Scalaz._
import java.io.File
import scala.io.Source
import org.specs2.specification.Fragments

package object sniff {
  implicit def snippetsToSniffer(snippets: CodeSnippets) = new Sniffer(snippets)
}

package sniff {

  case class CodeSnippets(language: Language, snippets: Smell*)
  case class Smell(regex: Regex, rationale: String)

  abstract class Language(val fileExtension: String)
  case object Scala extends Language("scala")
  
  class Sniffer(snippets: CodeSnippets) {
    import org.specs2.main.ArgumentsArgs._
    import org.specs2.specification.FragmentsBuilder._
    import MustMatchers._

    def sniff(paths: String*): Fragments = {
      val files = paths.map(path => getFileTree(new File(path))).reduce(_ ++ _)
      sniff(files)
    }

    def sniff(files: Stream[File]): Fragments = {
      var lineNum = 0
      def filter(file: File) = !file.isDirectory && file.getName().endsWith(".%s".format(snippets.language.fileExtension))
      val smells = for {
        file <- files if filter(file) ensuring { lineNum = 0; true }
        line <- Source.fromFile(file).getLines()
        snippet <- snippets.snippets
      } yield "%s:%s: failed snippet '%s' (%s)".format(file.getAbsolutePath(), lineNum, snippet.regex, snippet.rationale) ! { line must not be =~(snippet.regex.toString) } ensuring { lineNum = lineNum + 1; true }

      args(showOnly = "x!o*-1") ^ smells 
    }

    private def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)
  }
}
