package lectures.part4implicits

import scala.jdk.CollectionConverters._

object ScalaJavaConversions extends App {

  import scala.collection.mutable._

  val numbersBuffer = ArrayBuffer(1, 2, 3)
  val juNumbersBuffer = numbersBuffer.asJava
  println(numbersBuffer eq juNumbersBuffer.asScala)
  val numbers = List(1, 2, 3)
  val juNumbers = numbers.asJava
  val backToScala = juNumbers.asScala
  println(numbers eq backToScala)
  println(numbers == backToScala)
}
