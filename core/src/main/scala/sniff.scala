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
  
  implicit val smells: Smells = DefaultSmells

  /**
   * {{{
   *   val file: File
   *   val smell: Smell
   *   (file, smell) must smellNice
   * }}}
   */
  def smellNice: Matcher[(File, Smell)] = new SmellsNiceMatcher
  private[sniff] def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)
  
  implicit def snippetsToSniffer(snippets: CodeSnippets) = new Sniffer(snippets)
  
  class ToSnippets(lang: Language)(implicit smells: Smells) { 
    def snippets = CodeSnippets(lang.fileFilter, lang.smells: _*)
  }
  implicit def langToSnippets(lang: Language)(implicit smells: Smells): ToSnippets = new ToSnippets(lang)
  implicit def langToTag(lang: Language): Tag = lang.tag
  
  implicit def langToFilter(lang: Language): FileFilter = lang.fileFilter
  implicit def filenamesToFilter(named: FilesNamed): FileFilter = file => !file.isDirectory && named.filenames.exists(_ == file.getName())
  implicit def pathToFileFilter(path: Path): FileFilter = _.getAbsolutePath().endsWith(path)
  implicit def regexToFileFilter(regex: Regex): FileFilter = file => regex.findFirstIn(file.getAbsolutePath()).isDefined
}

package sniff {
  
  /*
   * Tag => Option[Language]
   * Tag => Seq[Smell]
   * 
   * Language => FileFilter
   * Language => Tag
   * 
   * FileFilter => Seq[Smell] => Snippets
   * 
   * Sniffer => Snippets => Seq[Path] => Specification
   */

  case class CodeSnippets(filter: FileFilter, smells: Smell*)
  case class Smell(id: SmellId, regex: Regex, rationale: String, tags: Tag*)
  case class Ignores(ignores: Ignore*) {
    def ignore(smell: Smell, file: File): Boolean = ignores.find(_.ignore(smell, file)).isDefined
  }
  /** Note that the package object defines implicit conversions from `Path` and `Regex` to `FileFilter`. */
  case class Ignore(id: SmellId, filters: FileFilter*) {
    def ignore(smell: Smell, file: File) = id == smell.id && filters.exists(_(file))
  }
  
  class SniffSpecification(title: String, snippets: Seq[CodeSnippets], paths: Seq[String]) extends Specification {
    def is = title.title ^ (Fragments() /: snippets)(_ ^ _.sniff(paths: _*))
  }
  
  object SniffSpecification {
    def apply(title: String, tags: Seq[Tag], paths: Seq[Path])(implicit smells: Smells): SniffSpecification = {
      val snippets = for {
        tag <- tags if Language(tag).isDefined
        l <- Language(tag)
      } yield CodeSnippets(l.fileFilter, l.smells: _*)
      new SniffSpecification(title, snippets, paths)
    }
  }

  case class Language(fileExtension: String, tag: Tag) {
    def fileFilter: FileFilter = { file: File => !file.isDirectory && file.getName().endsWith(".%s".format(fileExtension)) }
    def smells(implicit smells: Smells) = smells.withTags(_.contains(tag))
  }
  
  object Language {
    val Scala = Language("scala", 'Scala)
    val Java = Language("java", 'Java)
    val Php = Language("php", 'Php)
    
    val values = Scala :: Java :: Php :: Nil
    
    def apply(lang: Tag): Option[Language] = apply(lang.name)
    def apply(lang: String): Option[Language] = values.find(_.tag.name.toLowerCase() == lang.toLowerCase())
  }
  
  case class FilesNamed(filenames: String*)
  
  case class FileSmell(file: File, smell: Smell)
    
  private[sniff] class SmellsNiceMatcher extends Matcher[(File, Smell)] {
    def apply[FS <: (File, Smell)](s: Expectable[FS]) = {
      val found = findSmell(s.value._1, s.value._2)
      result(!found.isDefined,
          s.description + " is ok",
          failureMsg(s.value._2, s.value._1, found.map(_._2 + 1).getOrElse(-1)),
          s)
    }

    // TODO: find all, not find first
    private def findSmell(file: File, smell: Smell) = lines(file).find(l => smell.regex.findFirstIn(l._1).isDefined)
    private def lines(file: File) = Source.fromFile(file).getLines.zipWithIndex
    private def failureMsg(smell: Smell, file: File, line: Int) = "failed snippet %s at %s:%s (%s)".format(smell.id.name, file.getAbsolutePath(), line, smell.rationale)
  }
  
  class Sniffer(snippets: CodeSnippets) extends SpecificationFeatures {
    import org.specs2.specification.FragmentsBuilder._
    import MustMatchers._

    def sniff(paths: Path*)(implicit ignore: Ignores = Ignores()): Fragments = {
      val files = paths
          .map(path => getFileTree(new File(path)))
          .reduce(_ ++ _)
          .filter(!_.isDirectory())
      sniff(files).foldLeft(Fragments())(_ ^ _ ^ p)
    }

    def sniff(files: Stream[File])(implicit ignore: Ignores): Stream[Fragments] = 
      for {
    	file <- files if snippets.filter(file)
      } yield "%s should smell ok".format(file.getAbsolutePath()) ^ sniff(file)

    def sniff(file: File)(implicit ignore: Ignores): Seq[Example] = 
      for {
    	smell <- snippets.smells
      } yield {
    	val description = "%s (%s)".format(smell.id.name, smell.tags.mkString(", "))
      
        if (ignore.ignore(smell, file)) description ! skipped
    	else description ! ((file, smell) must smellNice) 
      }
  }
}
