package lectures.part5ts

object RockingInheritance extends App {
  // diamond problem
  trait Animal {
    def name(): String
  }
  trait Lion extends Animal {
    override def name(): String = "lion"
  }
  trait Tiger extends Animal {
    override def name(): String = "tiger"
  }
  /**
   * Mutant extends Animal with {override def name(): String = "lion"}
   * with Animal with {override def name(): String = "tiger"}
   *
   * LAST wins
   */
  class Mutant extends Lion with Tiger
  val m = new Mutant()
  println(m.name()) // tiger

  // super problem + type linearization
  trait Cold {
    def print(): Unit = println("cold")
  }

  trait Green extends Cold {
    override def print(): Unit = {
      println("green")
      super.print()
    }
  }

  trait Blue extends Cold {
    override def print(): Unit = {
      println("blue")
      super.print()
    }
  }

  class Red {
    def print(): Unit = println("red")
  }

  /**
   * Cold  = AnyRef with <Cold>(body of cold)
   * Green = Cold with <Green>
   *       = AnyRef with <Cold> with <Green>
   * Blue  = Cold with <Blue>
   *       = AnyRef with <Cold> with <Blue>
   * Red   = AnyRef with <Red>
   *
   * White = AnyRef with <Red>
   *    with AnyRef with <Cold> with <Green>
   *    with AnyRef with <Cold> with <Blue>
   *    with <White>
   *       = AnyRef with <Red> with <Cold> with <Green> with <Blue> with <White>
   * 去掉重复项，white的super为Blue，再上是Green，再上是Cold
   */
  class White extends Red with Green with Blue {
    override def print(): Unit = {
      println("white")
      super.print()
    }
  }

  val white = new White()
  white.print()
}
