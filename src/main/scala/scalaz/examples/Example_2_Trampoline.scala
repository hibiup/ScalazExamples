package scalaz.examples

/**
  * Trampoline 的原理其实也是尾递归。但是在实际编程中，有些复杂些的算法是无法用尾递归方式来实现的，加上JVM 实现 TCE 的能力有
  * 局限性，只能对本地（Local）尾递归进行优化。例如：
  *
    def even[A](as: List[A]): Boolean = as match {
        case Nil => true
        case h :: t => odd(t)
    }                  //> even: [A](as: List[A])Boolean
    def odd[A](as: List[A]): Boolean = as match {
        case Nil => false
        case h :: t => even(t)
    }                  //> odd: [A](as: List[A])Boolean
  *
  * 如果 odd 和 even 这两个函数的参数长度恰好和调用的方法相同，则返回 true，否则返回 false。这个两个函数互相调用，彼此的结尾调
  * 用了对方，编译器就无法实现尾递归。为了避免这个问题，我们需要解决两个问题：
  *
  * 1）让返回值具有统一的返回类类型。
  * 2）回避函数过程的复杂性，将尾递归的条件隔离出来单独考虑。
  *
  * 第一个问题的解决办法是用一个统一的返回类型：Trampoline[+A] 容器来装载执行结果（值或函数），这样就避免了编译器直接基于结果
  * 影响而无法优化成尾递归。
  *
  * 第二个问题的解决办法是将结果存放在容器中，然后将容器分为两种可能：Done(a)，代表完成运算并返回结果 a，或者 More(k)，表示需要
  * 递归运算，因为容器的隔离，使得递归的条件被隔离出来，就能实现广泛的尾递归条件兼容性。
  *
  * 因此有以下实现：
  */

object Example_2_Trampoline_1 {
    /** 1) 定义 Trampoline[+A] 容器，也就是以上第一条。 */
    trait Trampoline[+A] {
        /** 2）这个容器只考虑尾递归条件，解决以上第二条，*/
        final def runT: A = this match {
            case Done(a) => a
            case More(k) => k().runT
        }
    }

    /** 2-1）由 Trampoline[+A] 派生出两个类：Done 和 More。*/
    case class Done[+A](a: A) extends Trampoline[A]
    case class More[+A](k: () => Trampoline[A]) extends Trampoline[A]

    /**
      * 3）修改以上函数，应用 Trampoline
      */
    def even[A](as: List[A]): Trampoline[Boolean] = as match {
        case Nil => Done(true)
        case _ :: t => More(() => odd(t))   // 如果是 case _ :: t =>  odd(t) 则不能优化成尾递归。
    }    // 返回 Trampoline[Boolean]，恰好是 even 的返回类型，哪怕实际上调用的实 odd. 因此欺骗了编译器。

    def odd[A](as: List[A]): Trampoline[Boolean] = as match {
        case Nil => Done(false)
        case _ :: t => More(() => even(t))  // 如果是 case _ :: t =>  even(t) 则不能优化成尾递归。
    }   // 返回 Trampoline[Boolean]，恰好是 odd 的返回类型，哪怕实际上调用的实 even. 因此欺骗了编译器。


    /** 如果把两者统一，就能看出，其实还是一个尾递归．*/
    def compu[A](as:List[A]):Trampoline[Boolean] = as match {
        case Nil => Done(true)
        case _ :: t => compu(t)
    }
}

/** 以下是一个 Scalaz-zio 的 Trampoline。它用 IO 取代了 Trampoline 类。*/
object Example_2_Trampoline_2 {
    import scalaz.zio.IO
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
