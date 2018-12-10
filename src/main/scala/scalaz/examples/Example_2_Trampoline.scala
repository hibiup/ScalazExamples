package scalaz.examples

import scalaz.zio.IO

object Example_2_Trampoline_1 {
    /** Trampoline 代表一个可以一步步进行的运算。每步运算都有两种可能：Done(a),直接完成运算并返回结果a，或者More(k)运算k后进入下
      * 一步运算；下一步又有可能存在Done和More两种情况。
      *
      * Trampoline 的原理其实也是尾递归，它将函数包裹在容器里面，最后通过返回的容器来决定是否继续递归，这样就避免了编译器受结果影响
      * 而无法优化成尾递归。*/
    trait Trampoline[+A] {
        final def runT: A = this match {
            case Done(a) => a
            case More(k) => k().runT
        }
    }

    /** Done 和 More 继承自 Trampoline。*/
    case class Done[+A](a: A) extends Trampoline[A]
    case class More[+A](k: () => Trampoline[A]) extends Trampoline[A]

    /**
      * odd 和 even 这两个函数互相调用 n 次，如果 n 恰好和调用的方法相同，则返回 true，否则返回 false
      *
      * 通常情况下，因为 odd 和 even 是两个不同的函数，因此如果在结尾互相调用编译器是不能优化成尾递归的，但是现在我梦将它们
      * 的返回值（函数）包裹在 Trampoline（More 或 Done） 中，就统一了返回类型，因此编译器就不考虑容器内的类型的差别，而优
      * 化成了尾递归。
      **/
    def even[A](as: List[A]): Trampoline[Boolean] = as match {
        case Nil => Done(true)
        case _ :: t => More(() => odd(t))   // 如果是 case _ :: t =>  odd(t) 则不能优化成尾递归。
    }    // 返回 Trampoline[Boolean]，恰好是 even 的返回类型，哪怕实际上调用的实 odd. 因此欺骗了编译器。

    def odd[A](as: List[A]): Trampoline[Boolean] = as match {
        case Nil => Done(false)
        case _ :: t => More(() => even(t))  // 如果是 case _ :: t =>  even(t) 则不能优化成尾递归。
    }   // 返回 Trampoline[Boolean]，恰好是 odd 的返回类型，哪怕实际上调用的实 even. 因此欺骗了编译器。

    def compu[A](as:List[A]):Trampoline[Boolean] = as match {
        case Nil => Done(true)
        case _ :: t => compu(t)   // 如果是 case _ :: t =>  odd(t) 则不能优化成尾递归。
    }
}

/** 以下是一个 Scalaz-zio 的 Trampoline。它用 IO 取代了 Trampoline 类。*/
object Example_2_Trampoline_2 {
    def fibIO(n:Int, a:BigInt=0, b:BigInt=1):IO[Error, BigInt] = {
        /** Scalaz zio 提供了异步功能 */
        IO.now(a + b).flatMap {b2 =>
            println(s"Thread-id: ${Thread.currentThread().getId}")
            if(n > 0 )
                fibIO(n - 1, b, b2)
            else
                IO.now(a)
        }
    }

    /** 拿掉 IO.now 可以看得更清楚一些，但是 IO.now 是异步的，因此这就变成了一个同步版本。 */
    def fib(n:Int, a:BigInt=0, b:BigInt=1):IO[Error, BigInt] = {
        if(n > 0 )
            fib(n - 1, b, a+b)
        else
            IO.now(a)
    }
}
