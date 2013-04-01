package net.rosien

import scala.util.matching.Regex
import scalaz._
import Scalaz._
import java.io.File
import scala.io.Source

package object sniff {
  type SmellId = Symbol
  type Path = String
  type FileFilter = File => Boolean
  type Tag = Symbol
  type `ValidationNEL[Stink]`[A] = ValidationNEL[Stink, A]

  implicit val smells: Smells = DefaultSmells

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

  object SniffSpecification {
    def apply(title: String, tags: List[Tag], paths: Seq[Path])(implicit smells: Smells): List[Sniffer] =
      for {
        tag <- tags if Language(tag).isDefined
        l <- Language(tag)
      } yield new Sniffer(CodeSnippets(l.fileFilter, l.smells: _*))
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

  case class Stink(file: File, smell: Smell, line: Int)

  case class FileSmell(file: File, smell: Smell) {
    val description = "%s (%s)".format(smell.id.name, smell.tags.mkString(", "))

    // TODO: find all, not find first
   def sniff: Validation[Stink, FileSmell] = {
      val source = Source.fromFile(file)
      try source.getLines.zipWithIndex
        .find(l => smell.regex.findFirstIn(l._1).isDefined)
        .toFailure(this)
        .fail.map(l => Stink(file, smell, l._2)).validation
      finally source.close
    }

    def failureMsg(line: Int) = "failed snippet %s at %s:%s (%s)".format(smell.id.name, file.getAbsolutePath(), line, smell.rationale)
  }

  class Sniffer(snippets: CodeSnippets) {
    def sniff(paths: Path*)(implicit ignore: Ignores = Ignores()): Stream[FileSmell] = {
      val files = paths
          .map(path => getFileTree(new File(path)))
          .reduce(_ ++ _)
          .filter(!_.isDirectory())

      sniff(files)
    }

    def sniff(files: Stream[File])(implicit ignore: Ignores): Stream[FileSmell] =
      for {
      	file <- files if snippets.filter(file)
        sniffers <- sniff(file)
      } yield sniffers

    def sniff(file: File)(implicit ignore: Ignores): Seq[FileSmell] =
      for {
      	smell <- snippets.smells if !ignore.ignore(smell, file)
      } yield FileSmell(file, smell)
  }
}
