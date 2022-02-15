package part4_tech

import akka.actor.ActorSystem
import akka.stream.Materializer

object AdvancedBackpressure extends App {
  implicit val system = ActorSystem("AdvancedBackpressure")
  implicit val materializer = Materializer(system)
}
