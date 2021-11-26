package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, FanOutShape2, Materializer, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import java.util.Date

object MoreOpenGraphs extends App {
  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = Materializer(system)

  /**
   * max3 operator
   */
  val max3graph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)
  val max3sink = Sink.foreach[Int](x => println(s"Max is $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3shape = builder.add(max3graph)

      source1 ~> max3shape.in(0)
      source2 ~> max3shape.in(1)
      source3 ~> max3shape.in(2)
      max3shape.out ~> max3sink

      ClosedShape
    }
  )

  //max3RunnableGraph.run()

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)
  val transactionSource = Source(List(
    Transaction("123", "Paul", "Jim", 100, new Date()),
    Transaction("456", "Dan", "Jim", 100000, new Date()),
    Transaction("789", "Jim", "Alice", 7000, new Date())
  ))
  val bankProcess = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txnId => println(s"suspicious txn ID: $txnId"))

  val suspiciousTxnStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
    val txnIdExtractor = builder.add(Flow[Transaction].map(txn => txn.id))

    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor

    new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  }

  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val suspiciousTxnShape = builder.add(suspiciousTxnStaticGraph)
      transactionSource ~> suspiciousTxnShape.in
      suspiciousTxnShape.out0 ~> bankProcess
      suspiciousTxnShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  suspiciousTxnRunnableGraph.run()
}
