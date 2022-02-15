package lectures.part5ts

object FBoundedPolymorphism extends App {
  // - solution
  //trait Animal
  //trait CanBreed[A] {
  //  def breed(a: A): List[A]
  //}
  //
  //implicit class CanBreedOps[A](a: A) {
  //  def brd(implicit canBreed: CanBreed[A]): List[A] = canBreed.breed(a)
  //}
  //
  //class Dog extends Animal
  //object Dog {
  //  implicit object DogCanBreed extends CanBreed[Dog] {
  //    override def breed(a: Dog): List[Dog] = List()
  //  }
  //}
  //val dog = new Dog
  //dog.brd
  //class Cat extends Animal
  //object Cat {
  //  implicit object CatCanBreed extends CanBreed[Dog] {
  //    override def breed(a: Dog): List[Dog] = List()
  //  }
  //}
  //
  //val cat = new Cat
  //cat.brd wrong
  // - solution
  trait Animal[A] {
    def breed(a: A): List[A]
  }

  class Dog
  object Dog {
    implicit object DogAnimal extends Animal[Dog] {
      override def breed(a: Dog): scala.List[Dog] = List()
    }
  }
  implicit class AnimalOps[A](a: A) {
    def brd(implicit animal: Animal[A]) = animal.breed(a)
  }

  val dog = new Dog
  dog.brd

  class Cat
  object Cat {
    implicit object CatAnimal extends Animal[Dog] {
      override def breed(a: Dog): scala.List[Dog] = List()
    }
  }
  val cat = new Cat
  //cat.brd wrong
}
