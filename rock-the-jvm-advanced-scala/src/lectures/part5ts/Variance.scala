package lectures.part5ts

object Variance extends App {
  trait Animal
  class Cat extends Animal
  class Person extends Animal
  class GoodGuy extends Person
  class BadGuy

  class Container[-A] {
    def isGood(a: A): Boolean = true
  }

  val coll: Container[Person] = new Container[Person]
  coll.isGood(new GoodGuy)
}
