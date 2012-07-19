package net.rosien

import org.specs2.matcher._
import scala.util.matching.Regex
import scalaz._
import Scalaz._
import java.io.File
import scala.io.Source
import org.specs2.Specification
import org.specs2.specification.Example
import org.specs2.specification.Fragments
import org.specs2.SpecificationFeatures
import org.specs2.execute.Skipped
import org.specs2.execute.Result

package object sniff {
  type SmellId = Symbol
  type Path = String
  type FileFilter = File => Boolean
  type Tag = Symbol

  /**
   * {{{
   *   val file: File
   *   val smell: Smell
   *   (file, smell) must smellNice
   * }}}
   */
  def smellNice: Matcher[FileSmell] = new SmellsNiceMatcher
  
  implicit def snippetsToSniffer(snippets: CodeSnippets) = new Sniffer(snippets)
  
  implicit def langToFilter(lang: Language): FileFilter = { file: File => !file.isDirectory && file.getName().endsWith(".%s".format(lang.fileExtension)) }
  case class ToSnippets(lang: Language) {
    def snippets = CodeSnippets(lang, Smells.withTags(_.contains(lang.tag)): _*)
  }
  implicit def langToSnippets(lang: Language): ToSnippets = ToSnippets(lang)
  
  
  private[sniff] def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)

  implicit def fileSmellMatcherToPairMatcher(matcher: Matcher[FileSmell]): Matcher[(File, Smell)] = matcher ^^ { fs: (File, Smell) => FileSmell(fs._1, fs._2) }
  
  implicit def filenamesToFilter(named: FilesNamed): FileFilter = file => !file.isDirectory && named.filenames.exists(_ == file.getName())
  implicit def pathToFileFilter(path: Path): FileFilter = _.getAbsolutePath().endsWith(path)
  implicit def regexToFileFilter(regex: Regex): FileFilter = file => regex.findFirstIn(file.getAbsolutePath()).isDefined
}

package sniff {

  case class CodeSnippets(filter: FileFilter, smells: Smell*)
  case class Smell(id: SmellId, regex: Regex, rationale: String, tags: Tag*)
  case class Ignores(ignores: Ignore*) {
    def ignore(smell: Smell, file: File): Boolean = ignores.find(_.ignore(smell, file)).isDefined
  }
  /** Note that the package object defines implicit conversions from `Path` and `Regex` to `FileFilter`. */
  case class Ignore(id: SmellId, filters: FileFilter*) {
    def ignore(smell: Smell, file: File) = id == smell.id && filters.exists(_(file))
  }

  abstract class Language(val fileExtension: String, val tag: Tag)
  case object Scala extends Language("scala", 'Scala)
  case object Java extends Language("java", 'Java)
  case object Php extends Language("php", 'Php)
  
  case class FilesNamed(filenames: String*)
  
  case class FileSmell(file: File, smell: Smell)
    
  private[sniff] class SmellsNiceMatcher extends Matcher[FileSmell] {
    def apply[FS <: FileSmell](s: Expectable[FS]) = {
      val found = findSmell(s.value)
      result(!found.isDefined,
          s.description + " is ok",
          failureMsg(s.value.smell, s.value.file, found.map(_._2 + 1).getOrElse(-1)),
          s)
    }

    private def findSmell(fs: FileSmell) = lines(fs.file).find(l => fs.smell.regex.findFirstIn(l._1).isDefined)
    private def lines(file: File) = Source.fromFile(file).getLines.zipWithIndex
    private def failureMsg(smell: Smell, file: File, line: Int) = "failed snippet %s at %s:%s (%s)".format(smell.id.name, file.getAbsolutePath(), line, smell.rationale)
  }
  
  class Sniffer(snippets: CodeSnippets) extends SpecificationFeatures {
    import org.specs2.specification.FragmentsBuilder._
    import MustMatchers._

    def sniff(paths: Path*)(implicit ignore: Ignores = Ignores()): Fragments = {
      sniff(paths
          .map(path => getFileTree(new File(path)))
          .reduce(_ ++ _)
          .filter(!_.isDirectory())).foldLeft(Fragments())(_ ^ _ ^ p)
    }

    def sniff(files: Stream[File])(implicit ignore: Ignores): Stream[Fragments] = for {
      file <- files if snippets.filter(file)
    } yield "%s should smell ok".format(file.getAbsolutePath()) ^ sniff(file)

    def sniff(file: File)(implicit ignore: Ignores): Seq[Example] = for {
      smell <- snippets.smells
    } yield {
      val description = "%s (%s)".format(smell.id.name, smell.tags.mkString(", "))
      
      if (ignore.ignore(smell, file)) description ! skipped
      else description ! ((file, smell) must smellNice)
    }
    
  }
}
