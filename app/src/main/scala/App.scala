package net.rosien.sniff

import scalaz._
import Scalaz._
import org.rogach.scallop._

/** The launched conscript entry point */
class App extends xsbti.AppMain {
  case class Exit(val code: Int) extends xsbti.Exit
  def run(config: xsbti.AppConfiguration) = Exit(App.run(config.arguments))
}

object App {
  import net.rosien.sniff._
  import net.rosien.sniff.ScallopScalaz._

  class SniffConf(args: Array[String]) extends ScallopConf(args) {
    version("sniff %s (c) 2012 Adam Rosien".format(BuildInfo.version))
    banner("Usage: sniff [<options>] <path>...")
    footer("See https://github.com/arosien/sniff for more info.")

    implicit val langConverer = singleArgConverter(Language(_).get)
    implicit val tagConverter = listArgConverter(Symbol(_))

    val lang = opt[Language](
        "lang",
        descr = "Language to sniff: %s".format(Language.values.map(_.tag.name).mkString(", ")),
        required = true)

    val extraTags = opt[List[Tag]]("tags", descr = "Extra tags of smells to sniff").orElse(Nil.some)
    val langTags = lang.map(_.tag :: Nil)
    val tags = (langTags |@| extraTags) { (l, ts) => l |+| ts }

    val paths = trailArg[List[String]]("paths", descr = "paths to sniff", required = true)
  }

  def main(args: Array[String]) {
    System.exit(run(args))
  }

  def run(args: Array[String]): Int = {
    val conf = new SniffConf(args) // Exits if given --help or --version.

    val spec = SniffSpecification(
        "code shouldn't smell (tags: %s; paths: %s)".format(conf.tags().mkString(", "), conf.paths().mkString(", ")),
        conf.tags(),
        conf.paths())

    (0 /: specs2.run(spec))(_ + _.hasIssues.fold(1, 0))
  }
}

object ScallopScalaz {
  implicit val scallopFunctor: Functor[ScallopOption] = new Functor[ScallopOption] {
    def fmap[A, B](r: ScallopOption[A], f: A => B) = r map f
  }

  implicit val scallopBind: Bind[ScallopOption] = new Bind[ScallopOption] {
    def bind[A, B](a: ScallopOption[A], f: A => ScallopOption[B]) = a flatMap f
  }
}
