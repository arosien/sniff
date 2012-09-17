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
  
  def run(args: Array[String]): Int = {
    val opts = options(args.toList)
    
    // TODO: String => Language
    val result = specs2.run(Scala.spec)
    (0 /: result)(_ + _.hasIssues.fold(1, 0))
  }
  
  def main(args: Array[String]) {
    System.exit(run(args))
  }
  
  type OptionMatch = PartialFunction[List[String], (List[String], (Symbol, String))]
  val Param = """^--(\S+)=(.+)$""".r
    
  def arg(sym: Symbol, name: String): OptionMatch = {
    case Param(n, value) :: tail if n == name => tail -> (sym -> value) 
  }
  
  // stolen from StackOverflow, made better
  def options(args: List[String], optionMatchers: OptionMatch*): Map[Symbol, String] = {
    val default: PartialFunction[List[String], Map[Symbol, String]] = { 
      case Nil => Map()
      case Param(name, value) :: tail => sys.error("Unknown option '%s'".format(name))
      case arg :: tail => sys.error("Unknown arg '%s'".format(arg))
    }
    
    def wrap(optionMatch: OptionMatch): PartialFunction[List[String], Map[Symbol, String]] = optionMatch andThen {
      case (tail, m) => options(tail, optionMatchers: _*) + m
    }
    
    (optionMatchers.map(wrap _) :+ default).reduce(_ orElse _)(args)
  }
}

case class Exit(val code: Int) extends xsbti.Exit
