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
  type Tag = Symbol

  implicit def snippetsToSniffer(snippets: CodeSnippets) = new Sniffer(snippets)
  
  implicit def langToFilter(lang: Language): FileFilter = { file: File => !file.isDirectory && file.getName().endsWith(".%s".format(lang.fileExtension)) }
  case class ToSnippets(lang: Language) { 
    def snippets = CodeSnippets(lang, Smells.withTags(_.contains(lang.tag)): _*)
  }
  implicit def langToSnippets(lang: Language): ToSnippets = ToSnippets(lang)
  
  implicit def filenamesToFilter(named: FilesNamed): FileFilter = { file: File => !file.isDirectory && named.filenames.exists(_ == file.getName()) }
  
  private[sniff] def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)
}

package sniff {

  case class CodeSnippets(filter: FileFilter, smells: Smell*)
  case class Smell(id: SmellId, regex: Regex, rationale: String, tags: Tag*)
  case class Ignores(ignores: Ignore*)
  case class Ignore(id: SmellId, paths: Path*) {
    def ignores(smell: Smell, file: File) = id == smell.id && paths.exists(file.getAbsolutePath().endsWith(_))
  }

  abstract class Language(val fileExtension: String, val tag: Tag)
  case object Scala extends Language("scala", 'Scala)
  case object Java extends Language("java", 'Java)
  case object Php extends Language("php", 'Php)
  
  case class FilesNamed(filenames: String*)
  
  class Sniffer(snippets: CodeSnippets) {
    import org.specs2.specification.FragmentsBuilder._
    import MustMatchers._

    def sniff(paths: Path*)(implicit ignore: Ignores = Ignores()): Stream[Example] = {
      sniff(paths
          .map(path => getFileTree(new File(path)))
          .reduce(_ ++ _)
          .filter(!_.isDirectory()))
    }

    def sniff(files: Stream[File])(implicit ignore: Ignores) = for {
      file <- files if snippets.filter(file)
      smell <- snippets.smells
    } yield "%s should smell ok (%s: %s)".format(file.getAbsolutePath(), smell.id.name, smell.tags.mkString(", ")) ! 
        examples(file, smell, ignore).reduce(_ and _)

    private def examples(file: File, smell: Smell, ignore: Ignores) = for {
      (line, lineNum) <- Source.fromFile(file).getLines().zipWithIndex 
    } yield {
      val matchingRegex = =~(smell.regex.toString)
      line aka failureMsg(smell, file, lineNum + 1) must not be (ignored(file, smell)(ignore) ? matchingRegex.orSkip("ignored") | matchingRegex)
    }
    
    private def ignored(file: File, smell: Smell)(implicit ignore: Ignores) = ignore.ignores.exists(_.ignores(smell, file)) 
    
    private def failureMsg(smell: Smell, file: File, line: Int) = "failed snippet %s at %s:%s (%s)".format(smell.id.name, file.getAbsolutePath(), line, smell.rationale)
  }
}
