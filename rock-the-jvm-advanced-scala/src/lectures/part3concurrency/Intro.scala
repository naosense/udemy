package lectures.part3concurrency

import java.util.concurrent.Executors
import scala.concurrent.Promise

object Intro extends App {
  val aThread = new Thread(() => print("Run in parallel 2"))
  aThread.start()
  val aThreadHello = new Thread(() => (1 to 5).foreach(_ => println("hello")))
  val aThreadGreeting = new Thread(() => (1 to 15).foreach(_ => println("goodbye")))
  aThreadHello.start()
  aThreadGreeting.start()

  val pool = Executors.newWorkStealingPool()

  this.synchronized {
    println("hello world")
  }
  object Fingers extends Enumeration {
    type Finger = Value

    val Thumb, Index, Middle, Ring, Little = Value
  }

  val pair = 1 -> 2
}
