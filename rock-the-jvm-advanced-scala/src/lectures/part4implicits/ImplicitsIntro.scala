package lectures.part4implicits

object ImplicitsIntro extends App {
  val pair = "name" -> 1

  case class Person(name: String) {
    def greet = s"Hello, my name is $name"
  }

  implicit def fromStringToPerson(str: String): Person = Person(str)

  println("Peter".greet)
}
