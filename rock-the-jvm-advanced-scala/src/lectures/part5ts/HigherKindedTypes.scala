package lectures.part5ts

import scala.concurrent.Future

/**
 * 作用是去掉模板代码
 */
object HigherKindedTypes extends App {
  trait MyList[T] {
    def flatMap[B](f: T => B): MyList[B]
  }

  trait MyOption[T] {
    def flatMap[B](f: T => B): MyOption[B]
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  trait MyFuture[T] {
    def flatMap[B](f: T => B): MyFuture[B]
  }

  def multiply[A, B](la: List[A], lb: List[B]): List[(A, B)] =
    for {
      a <- la
      b <- lb
    } yield (a, b)

  def multiply[A, B](la: Option[A], lb: Option[B]): Option[(A, B)] =
    for {
      a <- la
      b <- lb
    } yield (a, b)

  def multiply[A, B](la: Future[A], lb: Future[B]): Future[(A, B)] =
    for {
      a <- la
      b <- lb
    } yield (a, b)

  // use HKT
  trait Monad[F[_], A] {
    def flatMap[B](f: A => F[B]): F[B]

    def map[B](f: A => B): F[B]
  }

  implicit class MonadList[A](list: List[A]) extends Monad[List, A] {
    override def flatMap[B](f: A => List[B]): List[B] = list.flatMap(f)

    override def map[B](f: A => B): List[B] = list.map(f)
  }

  implicit class MonadOption[A](option: Option[A]) extends Monad[Option, A] {
    override def flatMap[B](f: A => Option[B]): Option[B] = option.flatMap(f)

    override def map[B](f: A => B): Option[B] = option.map(f)
  }

  //def multiply[F[_], A, B](fa: Monad[F, A], fb: Monad[F, B]) =
  //  for {
  //    a <- fa
  //    b <- fb
  //  } yield (a, b)

  def multiply[F[_], A, B](implicit fa: Monad[F, A], fb: Monad[F, B]) =
    for {
      a <- fa
      b <- fb
    } yield (a, b)

  val empty = List()
  println(multiply(new MonadList(List(1, 2)), new MonadList(List("a", "b"))))
  println(multiply(new MonadOption(Some(2)), new MonadOption(Some("a"))))
  println(multiply(List(1, 2), List("a", "b")))
  println(multiply(Some(2), Some("a")))
}
