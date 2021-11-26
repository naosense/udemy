package part2_primer

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * 关于materializing value，要和流过stream中的element区分开，物化值通常和流的控制有关
 */
object MaterializingStreams extends App {
  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
  //val simpleMaterialValue: NotUsed = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)
  //val sumFuture: Future[Int] = source.runWith(sink)

  import system.dispatcher

  //sumFuture.onComplete {
  //  case Success(value) => println(s"The sum value $value")
  //  case Failure(exception) => println(s"ex is $exception")
  //}

  // choose materializing value
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach(println)
  // important !!!
  val graph: RunnableGraph[NotUsed] = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.left)
  //graph.run().onComplete {
  //  case Success(value) => println(s"success with value $value")
  //  case Failure(exception) => println(s"failed with exception $exception")
  //}

  // sugars
  //val sum: Future[Int] = Source(1 to 10).runWith(Sink.reduce(_ + _))
  //Source(1 to 10).runReduce(_ + _) // same above
  // backward
  //Sink.foreach(println).runWith(Source.single(42))
  // both
  //Flow[Int].map(x => x + 1).runWith(simpleSource, simpleSink)
}
