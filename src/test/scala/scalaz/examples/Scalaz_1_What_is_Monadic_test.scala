package scalaz.examples

import org.scalatest.FlatSpec

class Scalaz_1_What_is_Monadic_test extends FlatSpec{
    "Bag Monad test 1" should "" in {
        import scalaz.examples.Scalaz_1_What_is_Monad_1._

        /** map */
        assert(FullBag(1).map(x => x + 2) == FullBag(3))

        /** flatMap */
        val abc1 = FullBag(1) flatMap {a => FullBag(2) flatMap {b => FullBag(3) flatMap  {c => FullBag(a+b+c) }}}
        assert(abc1 == FullBag(6))

        /** for  */
        val abc2 = for {
               a <- FullBag(1)
               b <- FullBag(2)
               c <- FullBag(3)
             } yield a + b + c
        assert(abc2 == FullBag(6))

        /** Test string */
        val concatABC =
            for {
                a <- FullBag("Hello")
                b <- FullBag(", ")
                c <- FullBag("World")
                d <- FullBag("!")
             } yield ( a + b + c + d)
        assert(concatABC == FullBag("Hello, World!"))

        /** Test EmptyBag*/
        val emptyBag = for {
                a <- FullBag("Hello")
                b <- EmptyBag: Bag[String]
                c <- FullBag("World")
                d <- FullBag("!")
            } yield ( a + b + c + d)
        assert(emptyBag == EmptyBag)

        val emptyIntBag = for {
                a <- FullBag(1)
                b <- EmptyBag: Bag[Int]
                c <- FullBag(3)
            } yield ( a + b + c )
        assert(emptyIntBag == EmptyBag)

        FullBag(1).flatMap(a =>
            (EmptyBag: Bag[Int]).flatMap(b =>
                FullBag(3).map(c => a + b + c)))
    }

    "Bag Monad test 2" should "" in {
        import scalaz.examples.Scalaz_2_Logging_Example_2._

        println(Bag(1).map(x => x + 2).content)

        /** Test string */
        val concatABC =
            for {
                a <- Bag("Hello")
                b <- Bag(", ")
                c <- Bag("World")
                d <- Bag("!")
            } yield ( a + b + c + d)
        assert(concatABC.content == "Hello, World!")
    }
}
