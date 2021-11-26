package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, Materializer}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * Nonlinear components:
 *  - fan-out
 *  - fan-in
 *
 * Fan-out components:
 *  - Broadcast
 *  - Balance
 *
 * Fan-in components:
 *  - Zip/ZipWith
 *  - Merge
 *  - Concat
 */
object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = Materializer(system)

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)
  val output = Sink.foreach[(Int, Int)](println)

  // step 1: setting up the fundmendals for the graph
  val graph = RunnableGraph.fromGraph {
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // MUTATE builder
      import GraphDSL.Implicits._
      // step 2: adding the necessary components for the graph
      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, Int])

      // step 3: tying up the components
      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1
      zip.out ~> output

      // step 4: return a closed shape
      ClosedShape // builder -> immutable, freeze it
      // shape
    } // static graph
  } // runnable graph

  //graph.run()

  /**
   * exercise: feed a source into 2 sinks at the same time(hint: broadcast)
   */

  val firstSink = Sink.foreach[Int](x => println(s"First sink: $x"))
  val secondSink = Sink.foreach[Int](x => println(s"Second sink: $x"))
  val source2sinksGraph = RunnableGraph.fromGraph {
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // MUTATE builder
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))

      //input ~> broadcast
      //broadcast.out(0) ~> firstSink
      //broadcast.out(1) ~> secondSink

      // 可简写为下面
      // implicits port numbering
      input ~> broadcast ~> firstSink
      broadcast ~> secondSink

      ClosedShape
    }
  }
  //source2sinksGraph.run()

  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.foreach((x: Int) => println(s"Sink1: $x"))
  val sink2 = Sink.foreach((x: Int) => println(s"Sink2: $x"))

  val balanceGraph = RunnableGraph.fromGraph {
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge
      balance ~> sink2

      ClosedShape
    }
  }
  balanceGraph.run()
}
