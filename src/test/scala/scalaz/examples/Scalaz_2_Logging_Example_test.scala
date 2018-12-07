package scalaz.examples

import org.scalatest.FlatSpec

class Scalaz_2_Logging_Example_test extends FlatSpec {
    "Logging Monad" should "" in {
        import scalaz.examples.Scalaz_2_Logging_Example.Log

        println(Log(1).map(x => x + 2).content)
    }
}
