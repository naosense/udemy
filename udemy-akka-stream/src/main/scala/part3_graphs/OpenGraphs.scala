package part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}
import akka.stream.{FlowShape, Materializer, SinkShape, SourceShape}

object OpenGraphs extends App {
  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = Materializer(system)

  val firstSource = Source(1 to 10)
  val secondSource = Source(100 to 200)
  val sourceGraph = Source.fromGraph {
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat
      secondSource ~> concat

      SourceShape(concat.out)
    }
  }

  //sourceGraph.to(Sink.foreach(println)).run()

  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

  //firstSource.to(sinkGraph).run()
  /**
   * Challenge: complex flow
   * Write your own flow that is composed of two other flows
   * - one add 1
   * - one * 10
   */
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(_ * 10)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)

      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out)
    }
  )

  //firstSource.via(flowGraph).to(Sink.foreach(println)).run()

  /**
   * flow from sink to source
   */
  def fromSink2Source[A, B](sink: Sink[A, _], source: Source[B, _]): Flow[A, B, _] = {
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>

        val sinkShape = builder.add(sink)
        val sourceShape = builder.add(source)

        FlowShape(sinkShape.in, sourceShape.out)
      }
    )
  }

  val f = Flow.fromSinkAndSource(Sink.foreach(println), Source(1 to 10))
}
