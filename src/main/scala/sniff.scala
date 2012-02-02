package net.rosien

import org.specs2.matcher._
import scala.util.matching.Regex
import scalaz._
import Scalaz._
import java.io.File
import scala.io.Source
import org.specs2.specification.Example

package object sniff {
  type SmellId = Symbol
  type Path = String
  type FileFilter = File => Boolean

  implicit def snippetsToSniffer(snippets: CodeSnippets)(implicit ignore: Ignores = Ignores()) = new Sniffer(snippets)
  implicit def langToFilter(lang: Language): FileFilter = { file: File => !file.isDirectory && file.getName().endsWith(".%s".format(lang.fileExtension)) }
  implicit def filenamesToFilter(named: FilesNamed): FileFilter = { file: File => !file.isDirectory && named.filenames.exists(_ == file.getName()) }
}

package sniff {

  case class CodeSnippets(filter: FileFilter, snippets: Smell*)
  case class Smell(id: SmellId, regex: Regex, rationale: String)
  case class Ignores(ignores: Ignore*)
  case class Ignore(id: SmellId, paths: Path*)

  abstract class Language(val fileExtension: String)
  case object Scala extends Language("scala")
  case object Java extends Language("java")
  case object Php extends Language("php")
  
  case class FilesNamed(filenames: String*)
  
  class Sniffer(snippets: CodeSnippets)(implicit ignore: Ignores) {
    import org.specs2.specification.FragmentsBuilder._
    import MustMatchers._

    def sniff(paths: Path*): Stream[Example] = {
      sniff(paths
          .map(path => getFileTree(new File(path)))
          .reduce(_ ++ _)
          .filter(!_.isDirectory()))
    }

    def sniff(files: Stream[File]) = for {
      file <- files if snippets.filter(file)
      snippet <- snippets.snippets if 
          !ignore.ignores.exists(e => 
            e.id == snippet.id && 
            e.paths.exists(file.getAbsolutePath().endsWith(_)))
    } yield "%s smells ok (%s)".format(file.getAbsolutePath(), snippet.id.name) ! examples(file, snippet).reduce(_ and _)

    private def examples(file: File, snippet: Smell) = for {
      (line, lineNum) <- Source.fromFile(file).getLines().zipWithIndex 
    } yield line aka failureMsg(snippet, file, lineNum + 1) must not be =~(snippet.regex.toString) 
    
    private def failureMsg(smell: Smell, file: File, line: Int) = "%s:%s: failed snippet '%s' (%s)".format(file.getAbsolutePath(), line, smell.regex, smell.rationale)
    
    private def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)
  }
}
