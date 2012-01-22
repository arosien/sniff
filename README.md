Keeps your code fresh smelling.

```scala
import org.specs2.Specification

class SniffSpec extends Specification { 
  import net.rosien.sniff._
    
  val snippets = CodeSnippets(Scala,
    Smell("""java\.net\.URL""".r, rationale = "URL actually resolves hostnames over the network, use java.net.URI instead"))
              
  def is = "Code shouldn't smell" ^ snippets.sniff("src/main/scala", "src/test/scala")
}
```
