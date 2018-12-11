package scalaz.examples

import org.scalatest.FlatSpec

class Example_2_Trampoline_test extends FlatSpec{
    "Trampoline" should "avoid stack overflow" in {
        import scalaz.examples.Example_3_Trampoline_1._

        /** */
        println(even((1 to 100000).toList).runT)
        println(even((1 to 100001).toList).runT)
        println(odd((1 to 100000).toList).runT)
        println(odd((1 to 100001).toList).runT)
    }

    "A pure trampoline test" should "" in {
        import scalaz.examples.Example_3_Trampoline_1._
        println(FlatMap((1 to 1000000).toList).runT)
    }

    "Scala-zio trampoline" should "" in {
        import scalaz.examples.Example_3_Trampoline_ZIO._
        import scalaz.zio.RTS

        /** ZIO 尽可能将副作用推向外围（尽可能靠近 main），因为在测试环境中没有 scalaz.zio.App（trait App extends RTS）
          * 因此需要显示调用　RTS.unsafeRun 来执行。 */
        object Fib extends RTS {
            def apply(n:Int) = {
                println(unsafeRun(fibIO(n)))
            }
        }
        Fib(100000)
        fib(100000).map(println)
    }
}
