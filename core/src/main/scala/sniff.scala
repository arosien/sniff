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
  def smellNice: Matcher[(File, Smell)] = new SmellsNiceMatcher
  
  implicit def snippetsToSniffer(snippets: CodeSnippets) = new Sniffer(snippets)
  
  implicit def langToFilter(lang: Language): FileFilter = { file: File => !file.isDirectory && file.getName().endsWith(".%s".format(lang.fileExtension)) }
  class ToSnippets(lang: Language) { 
    def snippets = CodeSnippets(lang, Smells.withTags(_.contains(lang.tag)): _*)
  }
  implicit def langToSnippets(lang: Language): ToSnippets = new ToSnippets(lang)
  implicit def langToTag(lang: Language): Tag = lang.tag
  
  private[sniff] def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)

  implicit def filenamesToFilter(named: FilesNamed): FileFilter = file => !file.isDirectory && named.filenames.exists(_ == file.getName())
  implicit def pathToFileFilter(path: Path): FileFilter = _.getAbsolutePath().endsWith(path)
  implicit def regexToFileFilter(regex: Regex): FileFilter = file => regex.findFirstIn(file.getAbsolutePath()).isDefined
  
  implicit def langToSpec(lang: Language): ToSpec = new ToSpec(lang)
  class ToSpec(lang: Language) {
    def spec(extraPaths: Seq[String] = Nil) = new SniffSpecification("%s code".format(lang.tag.name), lang.snippets, lang.paths ++ extraPaths)
  }
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
  
  class SniffSpecification(title: String, snippets: CodeSnippets, paths: Seq[String]) extends Specification {
    def is = title.title ^ "shouldn't smell" ^ snippets.sniff(paths: _*)
  }

  case class Language(fileExtension: String, tag: Tag, paths: String*)
  object Language {
    val Scala = Language("scala", 'Scala, "src/main/scala", "src/test/scala")
    val Java = Language("java", 'Java, "src/main/java", "src/test/java")
    val Php = Language("php", 'Php)
    
    val values = Scala :: Java :: Php :: Nil
    
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
