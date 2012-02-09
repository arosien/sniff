package net.rosien.sniff

import scala.math.Ordering

object Smells {
  private implicit def langToTag(lang: Language): Tag = lang.tag
  private val * = '*
  
  implicit val smellIdOrdering: Ordering[SmellId] = Ordering.by(_.name)
  
  def withTags(f: Seq[Symbol] => Boolean) = smells.filter(smell => smell.tags == Seq(*) || f(smell.tags)).sortBy(_.id)
  
  val smells =
      // Scala
      Smell('NoSystemProperties, """System\.\w+Property\(""".r, "Use Scala-idomatic sys.props object", Scala) ::
      // Scala testing
      Smell('NoJUnit, """org\.junit""".r, "Use org.specs2, JUnit is so 1990s",      Scala, 'Testing) ::
      Smell('NoSpecs1, """\s*import\s+org\.specs\.""".r, "Use org.specs2 (not v1)", Scala, 'Testing) ::
      // Java, or calling Java from Scala
      Smell('NoSystemOutOrErr, """\bSystem\.(out|err)\.print""".r, "Use a logger",                                            Java, Scala) ::
      Smell('NoURL, """\bjava\.net\.URL\b""".r, "URL actually resolves hostnames over the network, use java.net.URI instead", Java, Scala) ::
      Smell('UseLogicalClock, """System\.currentTimeMillis""".r, "Use a logical clock, otherwise it's hard to test",          Java, Scala) ::
      Smell('NoDefaultSuperConstructors, """super\(\)""".r, "Never call a default super constructor",                         Java, Scala) ::
      Smell('NoThreadSleep, """Thread.sleep\(""".r, "Sleep when you're dead",                                                 Java, Scala) ::
      Smell('NoThreadSleep, """import\s*static\s*java.lang.Thread.sleep""".r, "Sleep when you're dead",                       Java, Scala) ::
      Smell('UseStringEquals, """==\s*\"""".r, "always compare strings via String#equals(String)",                            Java) ::
      Smell('UseStringEquals, """"\s*== """.r, "always compare strings via String#equals(String)",                            Java) ::
      // Formatting
      Smell('NoLongLines, """.{200,}""".r, "Your line is too long, make it shorter", *) ::
      Nil
}