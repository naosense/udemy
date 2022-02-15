package lectures.part4implicits

object PimpMyLibrary extends App {
  implicit class RichInt(val value: Int) {
    def isEven: Boolean = value % 2 == 0

    def sqrt: Double = Math.sqrt(value)

    def times(f: () => Unit): Unit = {
      for (_ <- 0 until value) {
        f()
      }
    }

    def *(lst: List[Int]): List[Int] = {
      var l = List[Int]()
      for (_ <- 0 until value) {
        l = lst ++ l
      }
      l
    }
  }

  println(42.sqrt)
  println(42.isEven)

  1 to 10

  import scala.concurrent.duration._

  3.seconds

  // compiler does not multiple implicit search
  implicit class RicherInt(richInt: RichInt) {
    def isOdd: Boolean = richInt.value % 2 != 0
  }

  implicit class RichString(value: String) {
    def asInt: Int = value.toInt

    def encrypt: String = value + 1
  }

  println(3 * List(1, 2))

  // equivalent to implicit class RichAltInt(value: Int)
  class RichAltInt(value: Int)

  implicit def intToRichAltInt(value: Int): RichAltInt = new RichAltInt(value)
}
