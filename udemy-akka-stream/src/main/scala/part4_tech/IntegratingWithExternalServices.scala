package part4_tech

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.MessageDispatcher
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import java.util.Date
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.{existentials, postfixOps}

/**
 * Flows that call external service
 * {{{
 *   mySource.mapAsync(parallelism = 4)(element => MyExternalService.externalCall(element))
 * }}}
 *
 * Useful when service is asynchronous
 *  - the Future are evaluated in parallel
 *  - the relative order is maintained
 *  - a lagging Future will stall entire stream
 *
 * {{{
 *   mySource.mapAsyncUnordered(...)
 * }}}
 *
 * Evaluate the Future on their own ExecutionContext
 * MapAsync works great with asking actors
 * {{{
 *   mySource.mapAsync(parallelism = 4)(myActor ? _)
 * }}}
 */
object IntegratingWithExternalServices extends App {
  implicit val system = ActorSystem("IntegratingWithExternalServices")
  implicit val materializer = Materializer(system)

  case class PageEvent(application: String, description: String, date: Date)

  val eventSource = Source(List(
    PageEvent("AkkaInfra", "Infrastructure broke", new Date()),
    PageEvent("FastDataPipeline", "Illegal elements in the data pipeline", new Date()),
    PageEvent("AkkaInfra", "A service stop respond", new Date()),
    PageEvent("SuperFrontend", "A button doesn't work", new Date())
  ))

  object PagerService {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rockjvm.com",
      "John" -> "john@rockjvm.com",
      "Lady Gaga" -> "ladygag@rockjvm.com"
    )

    // not recommend in practice
    // import system.dispatcher
    implicit val dispatcher: MessageDispatcher = system.dispatchers.lookup("dedicated-dispatcher")

    def processEvent(event: PageEvent): Future[String] = Future {
      val engineerIndex = (event.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val email = emails(engineer)

      println(s"sending $engineer at $email with event $event from service")
      email
    }
  }

  val infraEvents = eventSource.filter(_.application == "AkkaInfra")
  // guarantee the relative order of elements, if you don't need it, use mapAsyncUnordered
  val infraEventsSource: Source[String, NotUsed] = infraEvents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event))
  infraEventsSource.to(Sink.foreach(println)).run()

  class PagerActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case pageEvent: PageEvent =>
        sender() ! processEvent(pageEvent)
    }

    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rockjvm.com",
      "John" -> "john@rockjvm.com",
      "Lady Gaga" -> "ladygag@rockjvm.com"
    )

    // not recommend in practice
    // import system.dispatcher
    implicit val dispatcher: MessageDispatcher = system.dispatchers.lookup("dedicated-dispatcher")

    private def processEvent(event: PageEvent): Future[String] = Future {
      val engineerIndex = (event.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val email = emails(engineer)

      println(s"sending $engineer at $email with event $event from actor")
      email
    }
  }

  implicit val timeout: Timeout = Timeout(2 seconds)
  val pagerActor = system.actorOf(Props[PagerActor], "pageActor")
  val alternativePageEngineersEmails: Source[String, NotUsed] = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String])
  alternativePageEngineersEmails.to(Sink.foreach(println)).run()
  // don not confused mapAsync with async(boundary)
}
