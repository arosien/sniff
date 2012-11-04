Keeps your code fresh smelling: generate bad code smells specs2 specifications for any source language.

# Installation

`sniff` is available via the Maven Central repository and current supports Scala 2.9.0-1 and 2.9.1.

sbt:

```scala
libraryDependencies ++= Seq(
  "net.rosien" %% "sniff" % "0.7" % "test"
) 
```

maven:

```xml
 <dependency>
  <groupId>net.rosien</groupId>
  <artifactId>sniff_2.9.1</artifactId> 
  <version>0.7</version>
  <scope>test</scope>
</dependency>
```

[![Build Status](https://secure.travis-ci.org/arosien/sniff.png)](http://travis-ci.org/arosien/sniff)

# Usage

```scala
import org.specs2.Specification

class SniffSpec extends Specification { 
  import net.rosien.sniff._
    
  def is = "Scala code shouldn't smell" ^ Scala.snippets.sniff("src/main/scala", "src/test/scala")
}
```

Running this spec in sbt scans the directories `src/main/scala` and `src/test/scala` 
for bad code smells described by regular expressions:

```
> test
[info] Compiling 1 Scala source to /Users/arosien/asr/sniff/target/scala-2.9.1.final/test-classes...
[info] SniffSpec
[info] 
[info] Code shouldn't smell
[info] + /Users/arosien/asr/sniff/src/main/scala/sniff.scala smells ok
[info] + /Users/arosien/asr/sniff/src/test/scala/SniffSpec.scala smells ok
[info]  
[info] Total for specification SniffSpec
[info] Finished in 504 ms
[info] 2 examples, 0 failure, 0 error
[info] 
[info] Passed: : Total 2, Failed 0, Errors 0, Passed 2, Skipped 0
[success] Total time: 4 s, completed Jan 22, 2012 7:13:31 PM
```

If I add the string "java.net.URL" to the above code (to make the smell spec fail) I get:

```
> test
[info] SniffSpec
[info] 
[info] Code shouldn't smell
[info] + /Users/arosien/asr/sniff/src/main/scala/sniff.scala smells ok
[error] x /Users/arosien/asr/sniff/src/test/scala/SniffSpec.scala smells ok
[error]     /Users/arosien/asr/sniff/src/test/scala/SniffSpec.scala:8: failed snippet 'java\.net\.URL' (URL actually resolves hostnames over the network, use java.net.URI instead) '  // java.net.URL' matches '.*java\.net\.URL.*' (sniff.scala:40)
[info]  
[info] Total for specification SniffSpec
[info] Finished in 277 ms
[info] 2 examples, 1 failure, 0 error
[info] 
[error] Failed: : Total 2, Failed 1, Errors 0, Passed 1, Skipped 0
[error] Failed tests:
[error]   net.rosien.sniff.SniffSpec
[error] {file:/Users/arosien/asr/sniff/}default-28e91d/test:test: Tests unsuccessful
[error] Total time: 1 s, completed Jan 23, 2012 9:46:05 AM
```

If there are bad smells that you temporarily want to ignore you can define an implicit `Ignores` value:

```scala
// snippets.sniff() uses this implicit
implicit val ignore = Ignores(
      Ignore('NoURL, "src/test/scala/SniffSpec.scala"),
      ...)
```
# Defining your own smells

```scala
import org.specs2.Specification
import net.rosien.sniff._
    
val mySmells = 
  Smell('NoMutableCollections, """scala\.collection\.mutable""".r, rationale = "Immutable is better than mutable. - El Jefe", Scala, 'movieReferences) ::
  // more smells
  Nil
}

class SniffSpec extends Specification { 
  def is = "Die smells die" ^ CodeSnippets(Scala, mySmells: _*).sniff("src/main/scala", "src/test/scala")
}
```

There is a growing list of default smells for various languages defined at 
https://github.com/arosien/sniff/blob/master/src/main/scala/smells.scala. 
Please fork and send me a pull request to have yours included.

To define a new language just create a singleton object extending `net.rosien.sniff.Language`.

# Regular Expressions? WTF!?

There are lots of ways to model "bad code smells", from simple rules to deep language parsing.  `sniff` emphasizes:

* smells as regular expressions, because they are easy to write and test for any source language
* broadly scoped smells rather than complex predicates ("never use X")
* a simple exception mechanism to ignore false positives

For more complex rules and deeper analysis, try these great tools:

* [PMD](http://pmd.sourceforge.net/)
* [FindBugs](http://findbugs.sourceforge.net/)

# Credits

* `BadCodeSnippetsTestRunner` from https://github.com/wealthfront/kawala, probably inspired by Google.
* `specs2` specifications from etorreborre at http://etorreborre.github.com/specs2.

# TODO

* Magic token to ignore smells on a line or in a region, e.g., for use in commented-out code.
* Executable jar to sniff stuff from the command-line, e.g., for use in continuous integration systems.
* Sniff helper method for maven-style paths (src/main/scala, etc.)
