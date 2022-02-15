package lectures.part4implicits

object TypeClasses extends App {
  /**
   * 1. only for types we write
   * 2. ONE implementation
   */
  trait HTMLWritable {
    def toHtml: String
  }

  case class User(name: String, age: Int, email: String) extends HTMLWritable {
    override def toHtml: String = s"<div>$name ($age yo) <a href=$email/></div>"
  }

  private val john = User("john", 32, "john@hello.com")
  val html: String = john.toHtml
  println(html)

  // option2 pattern match
  /**
   * 1. lost type safety
   * 2. need to modify code every time
   * 3. still ONE implementation???(son classes of User may have different implementation)
   */
  object HTMLSerializerPM {
    def serialToHTML(value: Any): String = {
      value match {
        case User => ???
        case _ => ???
      }
    }
  }

  trait HTMLSerializer[T] {
    def serialize(value: T): String
  }

  implicit object UserSerializer extends HTMLSerializer[User] {
    override def serialize(value: User): String = s"<div>${ value.name } (${ value.age } yo) <a href=${ value.email }/></div>"
  }

  println(UserSerializer.serialize(john))

  // 1. we can define serializers for other type

  import java.util.Date

  object DateSerializer extends HTMLSerializer[Date] {
    override def serialize(value: Date): String = s"<div>now is ${ value.getTime }</div>"
  }
  println(DateSerializer.serialize(new Date()))

  // 2. we can define MULTIPLE serializers
  object PartialUserSerializer extends HTMLSerializer[User] {
    override def serialize(value: User): String = s"<div>${ value.name }</div>"
  }

  trait Equal[T] {
    def apply(a: T, b: T): Boolean
  }

  object UserEqual extends Equal[User] {
    override def apply(a: User, b: User): Boolean = a.name == b.name && a.age == b.age
  }

  object NameEqual extends Equal[User] {
    override def apply(a: User, b: User): Boolean = a.name == b.name
  }

  // part2
  object HTMLSerializer {
    def serialize[T](value: T)(implicit serializer: HTMLSerializer[T]): String =
      serializer.serialize(value)

    def apply[T](implicit serializer:HTMLSerializer[T]): HTMLSerializer[T] = serializer
    def apply[T](value: T)(implicit serializer:HTMLSerializer[T]): String = serializer.serialize(value)
  }

  implicit object IntSerializer extends HTMLSerializer[Int] {
    override def serialize(value: Int): String = s"<div>int is $value</div>"
  }

  println(HTMLSerializer.serialize(42))
  println(HTMLSerializer.serialize(john))
  println(HTMLSerializer[User].serialize(john))
  println(HTMLSerializer[User](john))

  trait MyTypeClassTemplate[T] {
    def action(value: T): String
  }

  object MyTypeClassTemplate {
    def apply[T](implicit instance: MyTypeClassTemplate[T]): MyTypeClassTemplate[T] = instance
  }

  implicit class HTMLEnrichment[T](value: T) {
    def toHTML(serializer: HTMLSerializer[T]) = serializer.serialize(value)
  }

  println(john.toHtml + " lala") // == new HTMLEnrichment[User](john).toHTML(UserSerializer)

  /**
   * - extend to new type
   * - choose implementation
   * - super expressive
   */

  println(2.toHTML(IntSerializer))

  case class Permission(value: String)
  implicit val defaultPermission: Permission = Permission("0744")

  val standardPermission = implicitly[Permission]
}
