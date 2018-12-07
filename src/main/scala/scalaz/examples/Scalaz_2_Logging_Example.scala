package scalaz.examples

import scalaz.Monad

/**
  * 这是个更精巧的例子，实现和例１类似的功能，但是在结构上更精巧些．
  *
  * 这个例子用 trait 来作为容器，同时提供 flatMap 等接口（调用 Monad.bind）。使得接口分工更明确。
  **/

object Scalaz_2_Logging_Example {
    /** 3）实现 Scalaz Monad 接口。这个 implicit object 将被 Log trait 隐式获得。*/
    implicit object LogMonad extends Monad[Log] {
        def point[T](k: => T): Log[T] = Log(k)
        def bind[T, U](log: Log[T])(f: T => Log[U]): Log[U] = f(log.content)
    }

    /** 1）用一个 trait 来取代 case class 作为容器模板（值存放在 content 中） */
    trait Log[+T] { self =>
        def content:T

        /** 2）trait 提供了 Scala 所需的 flatMap 等方法。它们其实最终指向 Scalaz Monad 接口。*/
        def flatMap[U](f: T => Log[U]): Log[U] = implicitly[Monad[Log]].bind(self)(f)
        def map[U](f: T => U): Log[U] = implicitly[Monad[Log]].map(self)(f)
    }

    /** 4）定义容器 object，无法 extends 自 trait，因为我们不打算将容器的类型确定下来。 */
    object Log{
        /** 4-1）实例化 trait，根据参数确定容器类型。 */
        def apply[T](c:T):Log[T] = new Log[T] {
            override def content: T = c
        }
    }
}
