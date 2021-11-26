package part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{BidiShape, ClosedShape, Materializer}

/**
 * bidirectional flows
 */
object BidirectionalFlows extends App {
  implicit val system = ActorSystem("BidirectionalFlows")
  implicit val materializer = Materializer(system)

  def encrypt(n: Int)(string: String) = string.map(c => (c + n).toChar)

  def decrypt(n: Int)(string: String) = string.map(c => (c - n).toChar)

  val bidirectionalGraph = GraphDSL.create() { implicit builder =>

    val encryptFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptFlowShape = builder.add(Flow[String].map(decrypt(3)))

    //BidiShape(encryptFlowShape.in, encryptFlowShape.out, decryptFlowShape.in, decryptFlowShape.out)
    BidiShape.fromFlows(encryptFlowShape, decryptFlowShape)
  }

  val unencryptedStrings = List("akka", "is", "awesome", "testing", "bidirectional", "flows")
  val unencryptedSource = Source(unencryptedStrings)
  val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val unencryptedSourceShape = builder.add(unencryptedSource)
      val encryptedSourceShape = builder.add(encryptedSource)
      val bidi = builder.add(bidirectionalGraph)
      val encryptSinkShape = builder.add(Sink.foreach[String](string => println(s"encrypt string $string")))
      val decryptSinkShape = builder.add(Sink.foreach[String](string => println(s"decrypt string $string")))

      unencryptedSourceShape ~> bidi.in1  ; bidi.out1 ~> encryptSinkShape
      decryptSinkShape <~ bidi.out2       ; bidi.in2  <~ encryptedSourceShape

      ClosedShape
    }
  )

  cryptoBidiGraph.run()
}
