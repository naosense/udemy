package lectures.part5ts

object Refection extends App {

  import scala.reflect.runtime.{universe => ru}
  import ru._

  def getTypeArgs[T](value: T)(implicit typeTag: TypeTag[T]) =
    typeTag.tpe match {
      case TypeRef(_, _, args) => args
      case _ => List()
    }

  class MyMap[A, B]
  println(getTypeArgs(new MyMap[String, Int]))

  def isSubType[A, B](implicit ta: TypeTag[A], tb: TypeTag[B]): Boolean =
    ta.tpe <:< tb.tpe

  trait Animal
  trait Dog extends Animal
  println(isSubType[Dog, Animal])
}
