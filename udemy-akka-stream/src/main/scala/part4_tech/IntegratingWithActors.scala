package part4_tech

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.{existentials, postfixOps}

object IntegratingWithActors extends App {
  implicit val system = ActorSystem("IntegratingWithActors")
  implicit val materializer = Materializer(system)

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just receive a string $s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just receive a number $n")
        sender() ! (2 * n)
      case _ =>
      // do nothing
    }
  }

  /**
   * actor as flow
   */
  val simpleActor = system.actorOf(Props[SimpleActor])
  val numberSource = Source(1 to 10)
  // actor as a flow
  implicit val timeout: Timeout = Timeout(2 seconds)
  // ask不加[Int]竟然不能正常运行！！！
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)
  //numberSource.via(actorBasedFlow).to(Sink.ignore).run()
  //numberSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run()

  /**
   * actor as source
   */
  val actorPoweredSource = Source.actorRef[Int](
    bufferSize = 10,
    overflowStrategy = OverflowStrategy.dropHead
  )
  val materializedActor: ActorRef = actorPoweredSource.to(Sink.foreach[Int](number => println(s"Actor powered get number $number"))).run()
  //materializedActor ! 10
  // terminate the source
  //materializedActor ! akka.actor.Status.Success("complete")

  //val source: Source[Any, ActorRef] = Source.actorRef(
  //  completionMatcher = {
  //    case Done =>
  //      // complete stream immediately if we send it Done
  //      CompletionStrategy.immediately
  //  },
  //  // never fail the stream because of a message
  //  failureMatcher = PartialFunction.empty,
  //  bufferSize = 100,
  //  overflowStrategy = OverflowStrategy.dropHead)
  //val actorRef: ActorRef = source.to(Sink.foreach(println)).run()
  //
  //actorRef ! "hello"
  //actorRef ! "hello"
  //
  //// The stream completes successfully with the following message
  //actorRef ! Done
  /**
   * actor as sink
   * - an init message
   * - an ack message to confirm reception
   * - a complete message
   * - a function to generate a message in case the stream throw an exception
   */
  case object StreamInit
  case object StreamAct
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("stream initialize!")
        sender() ! StreamAct
      case StreamComplete =>
        log.info("stream completed")
        context.stop(self)
      case StreamFail(ex) =>
        log.warning(s"stream failed with ex $ex")
      case message =>
        log.info(s"Message $message come to its final point")
        sender() ! StreamAct
    }
  }

  val destinationActor = system.actorOf(Props[DestinationActor], "destination-actor")
  val actorBasedSink: Sink[Any, NotUsed] = Sink.actorRefWithAck(
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAct,
    onFailureMessage = throwable => StreamFail(throwable)
  )
  Source(1 to 10).to(actorBasedSink).run()
}
