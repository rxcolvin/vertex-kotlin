import json.jsonToMap
import json.mapToJson
import uimodel.AppUI
import uimodel.FieldEditorUI
import uimodel.UIState
import validators.notEmptyString
import kotlin.reflect.KClass


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

interface Fields<FH : FieldHolder<*>> {
  val all_: List<FH>
}

data class EntityField<T, E, E_>(
    override val fieldMeta: Field<T>,
    val get: (E) -> T,
    val get_: (E_) -> T?,
    val set_: (E_, T?) -> Unit,
    val nullable: Boolean = false
) : FieldHolder<T>


class JsonException(msg: String) : RuntimeException(msg)

//TODO: Map Builder
class JsonMapper<E, E_, T, S : Any>(
    val entityField: EntityField<T, E, E_>,
    val fromJson: (S) -> T,
    val toJson: (T) -> S,
    val jsonType: KClass<S>
) : FieldHolder<T> by entityField{

  fun toJson(entity: E, map: MutableMap<String, Any?>): Unit {
    val v = entityField.get(entity)
    val msg = entityField.fieldMeta.validate.invoke(v)
    if (msg == null) {
      throw JsonException("")
    }
    map.put(fieldMeta.name, toJson(v))

  }

  fun fromJson(map: Map<String, Any?>, entityBuilder: E_): Unit {
    val v = map[fieldMeta.name]
    if (v != null) {
      val jsonValue = jsonType.safeCast(v)
      if (jsonValue == null) {
        throw JsonException("")
      } else {
        entityField.set_(entityBuilder, fromJson(jsonValue))
      }
    } else {
      if (entityField.nullable) {
        entityField.set_(entityBuilder, null)
      } else {
        throw JsonException("")
      }
    }
  }
}

data class EntityMeta<E, E_>(
    val name: String,
    val fields: Fields<EntityField<*, E, E_>>,
    val builderFactory: () -> E_,
    val builder2Entity: (E_) -> E,
    val entity2Builder: (E) -> E_
    )

data class JsonMeta<E, E_>(
    val name: String,
    val fields: Fields<JsonMapper<E, E_, *, *>>
)

class JsonParser<E, E_>(
    private val entityMeta: EntityMeta<E, E_>,
    private val jsonMeta: JsonMeta<E, E_>,
    private val jsonToMap: (String) -> Map<String, Any?>,
    private val mapToJson: (Map<String, Any?>) -> String
) {


  fun parse(text: String): E {
    val map = jsonToMap(text)
    val b = entityMeta.builderFactory()

    jsonMeta.fields.all_.forEach {
      it.fromJson(map, b)
    }


    return entityMeta.builder2Entity(b)
  }

  fun unparse(entity: E): String {
    val mm:MutableMap<String, Any?>  =  mutableMapOf()

    jsonMeta.fields.all_.forEach {
      it.toJson(entity, mm)
    }

    return mapToJson(mm)

  }
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



object fooMeta {
  open class FooFields<FH : FieldHolder<*>>(
      val name: FH,
      val age: FH
  ) : Fields<FH> {
    override val all_ = listOf<FH>(name, age)
  }

  val name = EntityField(
      fieldMeta = dd.name,
      get = Foo::name::get,
      get_ = Foo_::name::get,
      set_ = Foo_::name::set
  )

  val age = EntityField(
      fieldMeta = dd.age,
      get = Foo::age::get,
      get_ = Foo_::age::get,
      set_ = Foo_::age::set
  )

  val em = EntityMeta<Foo, Foo_>(
      name = "Foo",
      fields = FooFields(
          name = name,
          age = age
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

  val jm = JsonMeta<Foo, Foo_>(
      name = "foo",
      fields = FooFields(
          name = JsonMapper(
              entityField = fooMeta.name,
              fromJson = {it},
              toJson ={it},
              jsonType = String::class
          ),
          age = JsonMapper(
              entityField = fooMeta.age,
              fromJson = {it},
              toJson ={it},
              jsonType = Int::class
          )
      )
  )

  val jsonParser = JsonParser(
      entityMeta = em,
      jsonMeta = jm,
      jsonToMap = jsonToMap,
      mapToJson = mapToJson
  )
}


// Controllers
class EntityController<E, E_>(

) {

}

class FieldEditorController<T, E, E_>(
    private val ui: FieldEditorUI<T>,
    private val entityField: EntityField<T, E, E_>
) {
    init {
      ui.onUpdate = this::onUpdate
    }

    fun setEntityBuilder(eb: E_) {
      ui.uiState = UIState<T> (
          value = entityField.get_(eb)
      )
    }

    private fun onUpdate(value: T) : UIState<T> {
      return UIState<T> (
          value = value
      )
    }
}


data class Entity (
    val foo: String
)

class AppController(
    ui: AppUI,
    entityMeta: List<EntityMeta<*, *>>
) {

  init {

  }


  private fun onCreateEntity() {

  }

}

fun main(args: Array<String>) {

}
