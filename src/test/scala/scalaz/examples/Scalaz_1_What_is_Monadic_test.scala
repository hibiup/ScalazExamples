package scalaz.examples

import org.scalatest.FlatSpec

class Scalaz_1_What_is_Monadic_test extends FlatSpec{
    "AddMonad" should "" in {
        import scalaz.examples.Scalaz_1_What_is_Monad._

        /** map */
        assert(Bag(1).map(x => x + 2).content == 3)

        /** flatMap */
        val abc1 = Bag(1) flatMap {a => Bag(2) flatMap {b => Bag(3) flatMap  {c => Bag(a+b+c) }}}
        assert(abc1.content == 6)

        /** for  */
        val abc2: Bag[Int] = for {
               a <- Bag(1)
               b <- Bag(2)
               c <- Bag(3)
             } yield a + b + c
        assert(abc2.content == 6)
    }
}
