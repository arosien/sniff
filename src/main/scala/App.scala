package net.rosien.sniff

import org.specs2.Specification
import scalaz._
import Scalaz._

/** The launched conscript entry point */
class App extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) = Exit(App.run(config.arguments))
}

object App {
  import net.rosien.sniff._
  
  /** Shared by the launched version and the runnable version,
   * returns the process status code */
  def run(args: Array[String]): Int = {
    // TODO: cmd-line stuff
    // TODO: String => Language
    val result = specs2.run(Scala.spec)
    (0 /: result)( _ + _.hasIssues.fold(1, 0))
  }
  
  /** Standard runnable class entrypoint */
  def main(args: Array[String]) {
    System.exit(run(args))
  }
}

case class Exit(val code: Int) extends xsbti.Exit


