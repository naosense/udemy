package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, Materializer, OverflowStrategy, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip}

object GraphCycles extends App {
  implicit val system = ActorSystem("GraphCycles")
  implicit val materializer = Materializer(system)

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementFlowShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementFlowShape
    mergeShape <~ incrementFlowShape
    ClosedShape
  }

  /**
   * solution 1: merge prefer
   */
  val accelerator2 = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementFlowShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      Thread.sleep(1000)
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementFlowShape
    mergeShape.preferred <~ incrementFlowShape
    ClosedShape
  }
  //RunnableGraph.fromGraph(accelerator2).run()
  /**
   * solution 2: buffer
   */
  val accelerator3 = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementFlowShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating $x")
      Thread.sleep(1000)
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementFlowShape
    mergeShape <~ incrementFlowShape
    ClosedShape
  }
  //RunnableGraph.fromGraph(accelerator3).run()

  /**
   * Challenge: create a fan-in shape
   * - two inputs which will be fed into exactly one number(1 and 1)
   * - output will emit an infinite fib sequence based of these two numbers
   * 1, 2, 3, 5, 8 ...
   * Hint: use ZipWith and cycles
   */
  val fibGenerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val zip = builder.add(Zip[Int, Int])
    val mergePreferred = builder.add(MergePreferred[(Int, Int)](1))
    val fibLogic = builder.add(Flow[(Int, Int)].map { pair =>
      val last = pair._1
      val previous = pair._2
      Thread.sleep(100)
      (last + previous, last)
    })
    val broadcast = builder.add(Broadcast[(Int, Int)](2))
    val extractLast = builder.add(Flow[(Int, Int)].map(_._1))

    zip.out ~> mergePreferred ~> fibLogic ~> broadcast ~> extractLast
    mergePreferred.preferred <~ broadcast

    UniformFanInShape(extractLast.out, zip.in0, zip.in1)
  }

  val fiboGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val source1 = builder.add(Source.single(1))
      val source2 = builder.add(Source.single(1))
      val sink = builder.add(Sink.foreach(println))
      val fibo = builder.add(fibGenerator)

      source1 ~> fibo.in(0)
      source2 ~> fibo.in(1)
      fibo.out ~> sink

      ClosedShape
    }
  )

  fiboGraph.run()
}
