package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{FlowShape, Materializer, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {
  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = Materializer(system)

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach(println)
  val counter = Sink.fold[Int, String](0)((cnt, _) => cnt + 1)

  /**
   * a composite component (sink)
   * print all string which are lowercase
   * count the string that are short(< 5char)
   */
  val complexSinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[String](2))
      val lowerFlow = builder.add(Flow[String].filter(w => w == w.toLowerCase()))
      val shortFlow = builder.add(Flow[String].filter(_.length < 5))

      broadcast ~> lowerFlow ~> printer
      broadcast ~> shortFlow ~> counter

      SinkShape(broadcast.in)
    }
  )

  //wordSource.to(complexSinkGraph).run()
  val complexSinkGraph2 = Sink.fromGraph(
    GraphDSL.create(counter) { implicit builder =>
      counterShape =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[String](2))
        val lowerFlow = builder.add(Flow[String].filter(w => w == w.toLowerCase()))
        val shortFlow = builder.add(Flow[String].filter(_.length < 5))

        broadcast ~> lowerFlow ~> printer
        broadcast ~> shortFlow ~> counterShape

        SinkShape(broadcast.in)
    }
  )
  //val shortWordCountFuture = wordSource.toMat(complexSinkGraph2)(Keep.right).run()
  //
  //import system.dispatcher
  //
  //shortWordCountFuture.onComplete {
  //  case Success(value) => println(s"The total number of short string is : $value")
  //  case Failure(exception) => println(s"sth failed with $exception")
  //}

  val complexSinkGraph3 = Sink.fromGraph(
    GraphDSL.create(printer, counter)((printerShape, counterShape) => counterShape) { implicit builder =>
      (printerShape, counterShape) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[String](2))
        val lowerFlow = builder.add(Flow[String].filter(w => w == w.toLowerCase()))
        val shortFlow = builder.add(Flow[String].filter(_.length < 5))

        broadcast ~> lowerFlow ~> printerShape
        broadcast ~> shortFlow ~> counterShape

        SinkShape(broadcast.in)
    }
  )

  //val shortWordCountFuture3 = wordSource.toMat(complexSinkGraph3)(Keep.right).run()

  import system.dispatcher

  //shortWordCountFuture3.onComplete {
  //  case Success(value) => println(s"The total number of short string is : $value")
  //  case Failure(exception) => println(s"sth failed with $exception")
  //}

  /**
   * exercise: enhance flow, get count of elements through the flow
   */
  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val countSink = Sink.fold[Int, B](0)((counter, _) => counter + 1)
    Flow.fromGraph(
      GraphDSL.create(countSink) { implicit builder =>
        countSinkShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[B](2))
          val originalFlowShape = builder.add(flow)

          originalFlowShape ~> broadcast ~> countSinkShape
          FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(x => x)
  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.right).run()
  enhancedFlowCountFuture.onComplete {
    case Success(count) => println(s"The elements count through flow is $count")
    case Failure(exception) => println(s"sth failed with $exception")
  }
}
