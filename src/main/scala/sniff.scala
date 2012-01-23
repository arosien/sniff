package net.rosien

import org.specs2.matcher._
import scala.util.matching.Regex
import scalaz._
import Scalaz._
import java.io.File
import scala.io.Source
import org.specs2.specification.Example

package object sniff {
  implicit def snippetsToSniffer(snippets: CodeSnippets) = new Sniffer(snippets)
}

package sniff {

  case class CodeSnippets(language: Language, snippets: Smell*)
  case class Smell(regex: Regex, rationale: String)

  abstract class Language(val fileExtension: String)
  case object Scala extends Language("scala")
  
  class Sniffer(snippets: CodeSnippets) {
    import org.specs2.specification.FragmentsBuilder._
    import MustMatchers._

    def sniff(paths: String*): Stream[Example] = {
      sniff(paths
          .map(path => getFileTree(new File(path)))
          .reduce(_ ++ _)
          .filter(file => !file.isDirectory && file.getName().endsWith(".%s".format(snippets.language.fileExtension))))
    }

    def sniff(files: Stream[File]) = for {
      file <- files
      snippet <- snippets.snippets
    } yield "%s smells ok".format(file.getAbsolutePath()) ! examples(file, snippet).reduce(_ and _)

    private def examples(file: File, snippet: Smell) = for {
      (line, lineNum) <- Source.fromFile(file).getLines().zipWithIndex 
    } yield line aka failureMsg(snippet, file, lineNum + 1) must not be =~(snippet.regex.toString) 
    private def failureMsg(smell: Smell, file: File, line: Int) = "%s:%s: failed snippet '%s' (%s)".format(file.getAbsolutePath(), line, smell.regex, smell.rationale) 
    private def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)
  }
}
