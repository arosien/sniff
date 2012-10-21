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
  
  class SniffConf(args: Array[String]) extends ScallopConf(args) {
    version("sniff %s (c) 2012 Adam Rosien".format(BuildInfo.version))
    banner("Usage: sniff [<options>] <path>...")
    footer("See https://github.com/arosien/sniff for more info.")
    
    implicit val langConverer = singleArgConverter(net.rosien.sniff.Language(_).get)
    implicit val tagConverter = listArgConverter(Symbol(_))
    
    val lang = opt[net.rosien.sniff.Language](
        "lang", 
        descr = "Language to sniff: %s".format(Language.values.map(_.tag.name).mkString(", ")),
        required = true)
        
    val tags = opt[List[Tag]]("tags", descr = "Tags of smells to sniff")
        
    val allTags = for {
      l <- lang
      t <- tags
    } yield l.tag :: t
        
    val paths = trailArg[List[String]](
        "paths", 
        descr = "extra paths to sniff", 
        required = false)
        
    val allPaths = for {
      l <- lang
      p <- paths
    } yield l.paths ++ p
    
    validate (allTags) { t => t.isEmpty ? "No tags specified".left[Unit] | ().right }
    validate (allPaths) { p => p.isEmpty ? "No paths specified".left[Unit] | ().right }
  }
  
  def main(args: Array[String]) {
    System.exit(run(args))
  }
  
  def run(args: Array[String]): Int = {
    val conf = new SniffConf(args) // Exits if given --help or --version.
    
    val lang = conf.lang()
    val spec = lang.spec(conf.paths())
    
    val result = specs2.run(spec)
    (0 /: result)(_ + _.hasIssues.fold(1, 0))
  }
}
