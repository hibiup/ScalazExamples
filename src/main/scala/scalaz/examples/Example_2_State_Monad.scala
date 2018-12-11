package scalaz.examples

import scalaz.State

/**
  * 参考：https://www.cnblogs.com/tiger-xc/p/5038311.html
  *
  * 之前介绍过，Monadic typeclass 是FP 的编程范式，不同的 Monad 代表着不同的数据处理模型。State Monad 对应的模型是：泛函编程在
  * 大多数情况下使用不变量，但是这并不代表数据不可改变（数据的改变称为状态（state）改变）。State Monad 就是一种用于改变数据的范式。
  * 要注意的是，状态的改变未必等于必须使用变量，状态改变可以通过改变变量来实现，也可以通过生成新的不变量来实现，具体用什么方法取决于实现
  * 方式，State Monad 只是实现改变的范式，它只要求满足没有副作用，并不关心具体用什么来存储状态。
  *
  * State Monad 的定义是：
  *
  * 　　type State[S, A] = StateT[Id, S, A]
  *
  * 可以看出它是 StateT 的第一个数据类型是 identify（幺元） 的时候的特例．而 StateT 又是 IndexedStateT 的特例：
  *
  *     type StateT[F[_], S, A] = IndexedStateT[F, S, S, A]
  *
  * State 的第一个类型参数是状态值的类型；第二个类型参数是改变 State 的时候的（输入或输出）操作数的类型。State 的 apply 定义如下：
  *
  *　　def apply[S, A](f: S => (S, A)): State[S, A] = StateT[Id, S, A](f)
  *
  * 它接受一个之前的状态作为参数，返回操作后的新的状态＋操作数．以 State 自己为例看一下 apply 为什么这样定义: 因为 State 继承自
  * IndexedStateT, 这个基类中存在一个初始化状态的函数:
  *
  *   /** An alias for `apply` */
  *   def run(initial: S1)(implicit F: Monad[F]): F[(S2, A)] = apply(initial)
  *
  *   /** Calls `run` using `Monoid[S].zero` as the initial state */
  *   def runZero[S <: S1](implicit S: Monoid[S], F: Monad[F]): F[(S2, A)] = run(S.zero)
  *
  * 也就是说 runZero 为 apply 传入了 S.zero (零值) 作为状态的初始值. run 是幺元，与任何值结合都不改变值。这是 State Monad 最简单
  * 的使用案例. 接下来以一个栈为例子,我们定义两个函数: pop 和 push. 它们会改变一个数据栈(Stack)的状态(State)。看一下 State Monad
  * 是怎样使用的。
  *
  * */
object Example_2_State_Monad_1 {
    /** 1）定义个栈 (Stack) 类型 */
    type Stack[T] = List[T]

    /** pop 的返回类型是 State[Stack[T], T]，第一个类型是状态值的类型，第二个是操作数的类型．上面说过 State 最终继承自 IndexedStateT。
      * IndexedStateT 是一个 Monad，定义在 scalaz/package.scala 中，有 map, flatMap 等一系列方法。
      *
      * 我们定义两个操作函数，这两个函数都会改变 State Monad 的内部数据的状态 */
    def push[T](a: T): State[Stack[T], T] = State { _s => (a :: _s, a) }
    def pop[T]: State[Stack[T], T] = State { case h::t => (t, h) }

    //println(push(2))

    val prg = for {
        /** a 等价于 flatMap[S3, B](f: A => dexedStateT[F, S2, S3, B]) 中的参数 A */
        a <- push(2)  // a == 2
        b <- push(3)  // b == 3
        _ <- push(4)  // _ == 4
        d <- pop      // d == 4
        _ <- pop      // _ == 3， 返回前弹出了 3，使得 S == List(2)
    } yield d  /** yield d 等价于 map[B](f: A => B):State[S, B] 传入参数 3:A => 4:B，函数返回 (S, B) 也就是 (List(2), 4)
                *  参见下面的语法糖展开 */
    assert(prg.eval(List()) == 4)
    assert(prg.exec(List()) == List(2))

    val finalState = prg.run(List())  // State(List(2), 4)。run参数是幺元，不改变结果。

    // 等价于：
    val x = push(2).flatMap(a =>    // a == 2
        push(3).flatMap(b =>        // b == 3
            push(4).flatMap(_ =>    // _ == 4
                pop.flatMap(d =>    // d == 4
                    pop.map(_ =>    // _ == 3
                        d)))))      // map[B](f:(3) => 4 ):State[S, B] = (List(2), 4)
    assert(x.run(List()) == (List(2), 4))
}
