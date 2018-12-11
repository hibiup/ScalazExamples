package scalaz.examples

import org.scalatest.FlatSpec

class Example_2_State_Monad_test extends FlatSpec{
    "Stack State Monad" should "" in {
        import scalaz.examples.Example_2_State_Monad_1._
        println(finalState)  //
    }

    "Fibonacci State Monad" should "" in {
        import scalaz.examples.Example_2_State_Monad_fib._

        println(res)
    }
}
