import validators.notEmptyString

/**
 * Created by richard.colvin on 12/04/2017.
 */


data class Field<T>(
    val name: String,
    val description: String,
    val validate: (T) -> String? = { null }
)

interface FieldHolder<T> {
  val fieldMeta: Field<T>
}

data class EntityField<T, E, E_>(
    override val fieldMeta: Field<T>,
    val get: (E) -> T,
    val get_: (E_) -> T?,
    val set_: (E_, T?) -> Unit
) : FieldHolder<T>

data class JsonField<T, S>(
    override val fieldMeta: Field<T>,
    val fromJson : (S) -> T,
    val toJson  :(T) -> S
) : FieldHolder<T>


data class EntityMeta<E, E_, F : Fields<EntityField<*, E, E_>>>(
    val name: String,
    val fields: F,
    val builderFactory: () -> E_,
    val builder2Entity: (E_) -> E,
    val entity2Builder: (E) -> E_
)

data class JsonMeta<F : Fields<JsonField<*,*>>>(
    val name: String,
    val fields: F
)

class JsonParser<E, E_, F : Fields<*>>(
    private val entityMeta: EntityMeta<E, E_, F>,
    private val jsonMeta: JsonMeta<F>,
    private val jsonToMap: (String) -> Map<String, Any?>,
    private val mapToJson: (Map<String, Any?>) -> String
) {

  private val fieldMap = ??
  fun parse(text: String): E {
    return entityMeta.builder2Entity(entityMeta.builderFactory())
  }

  fun unparse(entity: E): String = ""
}

object dd {
  val name = Field<String>(
      name = "name",
      description = "name desc",
      validate = ::notEmptyString
  )

  val age = Field<Int>(
      name = "age",
      description = "age desc",
      validate = { if (it > 0) null else "must be positive" }
  )
}

data class Foo(
    val name: String,
    val age: Int
)

data class Foo_(
    var name: String? = null,
    var age: Int? = null
)

typealias JsonInt = Int
typealias JsonString = String

interface Fields<FH : FieldHolder<*>> {
  val all_: List<FH>
}

object fooMeta {
  open class FooFields<FH : FieldHolder<*>>(
      val name: FH,
      val age: FH
  ) : Fields<FH> {
    override val all_ = listOf<FH>(name, age)
  }


  val em = EntityMeta<Foo, Foo_>(
      name = "Foo",
      fields = FooFields(
          name = EntityField(
              fieldMeta = dd.name,
              get = Foo::name::get ,
              get_ = Foo_::name::get ,
              set_ = Foo_::name::set
          ),
          age = EntityField(
              fieldMeta = dd.age,
              get = { it.age },
              get_ = { it.age },
              set_ = { it, v -> it.age = v }
          )
      ),
      builderFactory = { Foo_(null, null) },
      builder2Entity = {
        Foo(
            it.name!!,
            it.age!!
        )
      },
      entity2Builder = {
        Foo_(
            it.name,
            it.age
        )
      }
  )

  val jm = JsonMeta(
      name = "foo",
      fields = FooFields<JsonField<*, *>>(
          name = JsonField(
              fieldMeta = dd.name,
              fromJson = {it},
              toJson = {it}
          ),
          age = JsonField(
              dd.age,
              {it},
              {it}
          )
      )
  )

  val jsonParser = JsonParser (
      entityMeta= em,
      jsonMeta = jm
  )
}

fun main(args: Array<String>) {

}
