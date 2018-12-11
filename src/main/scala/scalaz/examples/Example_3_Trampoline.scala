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

object Example_3_Trampoline_1 {
    /** 1) 定义 Trampoline[+A] 容器，这个容器只考虑尾递归条件，解决以上第二条。因此需要注意的是，这个 runT 函数真正启动了递归的执行． */
    trait Trampoline[+A] {
        final def runT: A = this match {
            /**
              * 5）Done 或 More 本身只是一个存有函数( ()=>odd() 或 ()=>even() )的容器，直到容器内的函数 () 被执行后返回新的
              * 容器，然后再次被 runT 驱动去完成下一步。
              * */
            case More(k) => k().runT   // 调用 More中的无参函数
            case Done(a) => a
        }
    }

    /** 2）由 Trampoline[+A] 派生出两个类：Done 和 More。Done 或 More 本身只是一个存有函数( ()=>odd() 或 ()=>even() )的容器，
      *    它们本身不会主动执行，直到 Trampoline.runT 提取出容器内的函数并显式调用“()” 才被执行。*/
    case class Done[+A](a: A) extends Trampoline[A]
    case class More[+A](k: () => Trampoline[A]) extends Trampoline[A]

    /**
      * 3）对函数应用 Trampoline，让不同的函数都返回 Trampoline，也就是满足以上第一条。
      */
    def even[A](as: List[A]): Trampoline[Boolean] = as match {
        /** 4-1）返回含有无参函数容器More。如果是 case _ :: t =>  odd(t) 则不能优化成尾递归。
          *     More 中的无参函数实际指向 odd 函数，因此当这个函数被执行的时候，实际返回了 odd(t) 的返回值。（又一个Done 或 More）*/
        case _ :: t => More(() => odd(t))
        /** 4-2) */
        case Nil => Done(true)
    }    // 返回 Trampoline[Boolean]，恰好是 even 的返回类型，哪怕实际上调用的实 odd. 因此欺骗了编译器。

    /** 3-2) 同上*/
    def odd[A](as: List[A]): Trampoline[Boolean] = as match {
        case Nil => Done(false)
        case _ :: t => More(() => even(t))
    }


    /** 另）如果把两者合并，就能得到一个 Trampoline 版的 flatMap．*/
    def FlatMap[A](as:List[A]):Trampoline[Boolean] = as match {
        case Nil => Done(true)
        case _ :: t => More(() => FlatMap(t))
    }
}

/**
  * 以一个更复杂的 State Monad 为例
  **/
object Example_3_Trampoline_StateMonad {
    case class State[S, +A](runS: S => (A, S)) {
        import State._

        def flatMap[B](f: A => State[S, B]): State[S, B] = State[S, B] { s => {
            val (a1,s1) = runS(s)
            f(a1) runS s1
        }}

        def map[B](f: A => B): State[S,B] = flatMap( a => unit(f(a)))
    }

    object State {
        def unit[S,A](a: A) = State[S,A] { s => (a,s) }
        def getState[S]: State[S,S] = State[S,S] { s => (s,s) }
        def setState[S](s: S): State[S,Unit] = State[S,Unit] { _ => ((),s)}
    }
}


/** 以下是一个 Scalaz-zio 的 Trampoline。它用 IO 取代了 Trampoline 类。*/
object Example_3_Trampoline_ZIO {
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
