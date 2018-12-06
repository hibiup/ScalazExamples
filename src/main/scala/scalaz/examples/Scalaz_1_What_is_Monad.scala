package scalaz.examples

import scalaz.Monad

/**
  * FP 编程和 OOP 编程最大的区别就是 FP 编程要求数据在某种容器（context）里进行状态转变（transformation）。形象点表达就是F[T]。
  * F[] 就是各种独特的壳子（context）而 T 就是需要运算转变 U 的某种类型值。FP 程序的结果形象描述就好像F[T] => F[U]： 代表在F[]
  * 壳子内对T 进行运算，并把结果 U 保存在F[]内。Scalaz 通过 Functor, Applicative, Monad 提供了三种基本的函数施用方式，它们都
  * 是针对F[T]里的T 值：
  *
  * 1、 Functor    :  map[T,U]    (F[T])(f: T => U):  F[U]
  * 2、 Applicative:  ap[T,U]     (F[T])(f: F[T => U]): F[U]
  * 3 、Monad      :  flatMap[T,U](F[T])(f: T => F[U]): F[U]
  *
  * 所以可以说 Monadic typeclass 提供了规范的FP 编程框架（template）,程序员可以使用这些框架进行FP编程。（Monadic typeclass 不
  * 是数据类型，而是代表着某些编程的模式）
  */

/**
  * scalaz typeclass 分成模板和特质（trait）:
  *
  * 模板是针对特定运算模式，比如 Functor, Applicative, Monad 都代表不同的编程方式或者说它们都具备不同的程序运算模式。它们被作为基类
  * 模板继承下来。trait 是指不同的数据类型所定义的 typeclass，它实例控制着程序的具体运算行为。
  */

/**
  * 如以 Option Functor 为例。它可以在 None 状态时中途终止运算，因为它的特质基于不同类型的实例而表现不同的运算行为，它的定义如下：
  *
  * implicit object OptionFunctor extends Functor[Option] {
  *     def map[T,U](ot: Option[T])(f: T => U): Option[U] = ot match {
  *         case Some(t) => Some(f(t))  // 将函数作用于容器成员 t．作用函数是直接以函数参数的形式提供的。
  *         case None => None
  *     }
  * }
  * Option Functor 的驱动函数 map 的意思是说如果目标类型 F[T] 的值是个 Some，那么我们就在 Some 壳内施用参数提供的一般函数f；
  * 如果目标值是 None 就不施用函数。
  *
  * ------------
  * 再看看 Option Applicative 的实例
  *
  * implicit object OptionApplicative extends Applicative[Option] {
  *     def point[T](t: T): Option[T] = Some(t)
  *     def ap[T,U](ot: Option[T])(of: Option[T => U]): Option[U] = (ot, of) match {
  *         case (Some(t), Some(f)) => Some(f(t))　　// 函数参数是装载容器里提供的。其它条件和 map 一样。
  *         ase _ => None
  *     }
  * }
  * Option Applicative 的驱动函数 ap 凸显了Option 的特别处理方式：只有在目标值和操作函数都不为 None 时才施用通过壳提供的操作函数。
  *
  * ------------
  * 再看看 Option Monad 实例：
  *
  * implicit object OptionMonad extends Monad[Option] {
  *     def flatMap[T,U](ot: Option[T])(f: T => Option[U]): Option[U] = ot match {
  *         case Some(t) => f(t)    // flatMap 接受的函数和 map 的区别在于，map 只负责值映射，而 flatMap 还要负责重新装箱。
  *         case _ => None
  *     }
  * }
  * 因为 flatMap 会对结果进行重新打包装箱，因此将它应用于多个容器（F[T]）时会最终得到一个输出容器（F[V]），而不像 map 那样会得到多
  * 个输出容器。也就是说 map 不管容器,只进行容器内的值的映射，而 flatMap 不仅映射值，还会将逐个映射结果取出最后打包成一个容器输出。
  * 因为：f: T => Option[U] 即为给一个T 值进行计算后产生另一个 Option[U]，如果再给 Option[U] 一个值进行计算的话就又会产生另一个
  * 运算 Opton[V]... 如此持续：F[A](a => F[B](b => F[C](c => F[D])...))。用 flatMap 链表示：
  *
  *   fa.flatMap(a => fb.flatMap(b => fc.flatMap(c => fd.map(...))))
  *
  * 从 flatMap 串联可以观察到因为重新打包的存在，所以不会出现一个结果包着另一个结果的情况，这特别符合 Monad 运算的关联依赖性和串联要
  * 求。并且针对于 Option Monad 来说，如果前面的运算产生结果是 None 的话，串联运算就终止并直接返回 None 作为整串运算的结果。
  *
  * 值得提醒的是连串的 flatMap 其实是一种递归算法（只不过作用于不同的函数），所以，直接使用 Monad 编程是不安全的，必须与 Trampling
  * 数据结构配合使用才行。正确安全的 Monad 使用方式是通过 Trampling 结构存放原本在堆栈上的函数调用参数，以 heap 替换 stack 来防止
  * stack-overflow。
  */

/**
  * 下面以一个加法运算为例具体讲解 Monadic 编程，这个例子的需求是：
  *
  * val a = 1
  * val b = 2
  * val c = 3
  * val d = a + b + c
  *
  * Monadic 编程是一种将数据放在上下文（context）中然后将其放在“传送带”上进行传递的流式运算模型，因此我们首先需要建立用于装载操
  * 作数的“容器”（高阶类型），然后建立操作该容器的 Monad，并实现向对应操作的方法。具体的实现如下：
  * */
object Scalaz_1_What_is_Monad {
    /** 1) 新建容器(数据类型为泛型) */
    trait Bag[+A]

    /** 1-1）新建两个 case 分别代表容器中存在和不存在（Nothing）数据两种状态 */
    case class FullBag[T](content:T) extends Bag[T]
    case object EmptyBag extends Bag[Nothing]         // Nothing 是唯一实例

    object Bag {
        /** 2）数据装箱 */
        //def apply[A](a: A) = new FullBag(a)

        /**
          * 3）以 Monad trait 为基类，实现具体容器的 Monad。但是注意这个 Monad 不是 implicit class，它不与具体容器实例绑定。
          *    它只是一个包含操作的类，因此它被定义为 implicit object，意为在系统中同时隐式建立了一个 Monad[Bag]，这个实例只包
          *    含操作，不包含目标容器，它将在第 4 步被隐式做用于目标容器。
          *
          * */
        implicit object BagMonad extends Monad[Bag] {
            /** 3-1） point 的目的是把操作数装入高阶类型中以便于后面的操作。 */
            def point[A](a: => A) = FullBag(a)

            /** 3-2）bind 既是 flatMap，是具体执行数据操作的场所。
              *
              * 根据 flatMap 的定义，它得到一个目标容器 bag，和对该容器的操作函数 f，然后将之施于 bag 上。最后返回结果容器。
              * */
            def bind[A, B](bag: Bag[A])(f: A => Bag[B]): Bag[B] = bag match {
                case FullBag(a) => f(a)   // 如果存在数据，则计算新的容器
                case _ => EmptyBag        // 否则返回表示空的容器，会导致计算链条中止并返回 EmptyBag。
            }
        }

        /** 4) 以隐式实现对容器的绑定，这个类将调用 Monad[Bag] 来完成运算。 */
        implicit class BagMonadOps[A](b:Bag[A]) {
            /** 4-1）实现 Monad。
              *
              * 隐式将第 3 步定义的 implicit object 实例作为参数。*/
            def flatMap[B](f: A => Bag[B])(implicit monad: Monad[Bag]): Bag[B] = monad.bind(b)(f)

            /** 4-2) 实现 Functor */
            def map[B](f: A => B)(implicit monad: Monad[Bag]): Bag[B] = monad.map(b)(f)
        }
    }

}
