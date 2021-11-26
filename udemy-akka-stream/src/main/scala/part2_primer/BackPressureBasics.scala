package part2_primer

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Data flows through streams in response to '''consumer''' demand
 *
 * Akka streams can slow down fast producers.
 *
 * Backpressure protocol is transparent.
 */
object BackPressureBasics extends App {
  implicit val system = ActorSystem("BackPressureBasics")
  implicit val materializer = Materializer(system)

  val fastSource = Source(1 to 100)
  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  // because fusion, no backpressure
  //fastSource.to(slowSink).run()

  // do have backpressure
  //fastSource.async.to(slowSink).run()

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming $x")
    x + 1
  }
  //fastSource.async.via(simpleFlow).async.to(slowSink).run()

  /**
   * reactions to backpressure:
   *  - try to slow down if possible
   *  - buffer elements until there's more demand
   *  - drop down elements if overflows
   *  - tear down/kill the whole stream(failure)
   */
  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.fail)
  //fastSource.async.via(bufferedFlow).async.to(slowSink).run()

  /**
   * 1-16: buffered by sink, nobody is backpressure
   * 17-26: flow will buffer, flow will start dropping at the next element
   * 26-100: flow will always drop the oldest elements
   * => 91-100 => 92-101 => sink
   */

  /**
   * overflow strategy:
   *  - drop head = oldest
   *  - drop tail = newest
   *  - drop new = exactly element to be added = keep the buffer
   *  - backpressure
   *  - fail
   */

  fastSource.throttle(2, 1 second).runWith(Sink.foreach(println))
}
