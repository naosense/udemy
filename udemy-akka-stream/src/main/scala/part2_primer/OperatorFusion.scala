package part2_primer

import akka.actor.{Actor, ActorSystem}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

/**
 * Akka stream components are fused = run on the same actor
 *
 * Async boundaries:
 *  - components run on different actors
 *  - better throughput
 *
 *  Best when: individual operations are expensive
 */
object OperatorFusion extends App {
  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = Materializer(system)

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](x => println(s"timestamp ${ System.currentTimeMillis() }: $x"))
  // 在同一个actor上运行(default)
  //simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()

  // 相当于下面的操作
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case x: Int =>
        // flow operation
        val x2 = x + 1
        val x3 = x2 * 10
        // sink operation
        println(x3)
    }
  }

  //val simpleActor = system.actorOf(Props[SimpleActor])
  //(1 to 1000).foreach(simpleActor ! _)
  val complexFlow = Flow[Int].map { x =>
    val start = System.currentTimeMillis()
    Thread.sleep(1000)
    println(s"#1 start $start end ${ System.currentTimeMillis() }: $x -> ${ x + 1 }")
    x + 1
  }
  val complexFlow2 = Flow[Int].map { x =>
    val start = System.currentTimeMillis()
    Thread.sleep(1000)
    println(s"#2 start $start end ${ System.currentTimeMillis() }: $x -> ${ x * 10 }")
    x * 10
  }
  //simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()
  // Q: 一个疑问：流中元素计算一个完了才能下一个，在多个actor上运行为啥可以降低延迟
  // A: 不加async，2必须等到1执行完complexFlow2后才能开始，加了async，2等到1执行完complexFlow就可以开始了
  // 1--------complexFlow------1---------complexFlow2------
  //                           2---------complexFlow-------2--------complexFlow2--------
  simpleSource.via(complexFlow).async // run on one actor
    .via(complexFlow2).async // run another actor
    .to(simpleSink).run() // run on a third actor

  // ordering guarantees
  //Source(1 to 3)
  //  .map(ele => { println(s"Flow A: $ele -> ${ ele + 1 }"); ele + 1 }).async
  //  .map(ele => { println(s"Flow B: $ele -> ${ ele * 2 }"); ele * 2 }).async
  //  .map(ele => { println(s"Flow C: $ele -> $ele"); ele }).async
  //  .runWith(Sink.ignore)
}
