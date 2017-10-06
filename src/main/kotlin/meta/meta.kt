/**
 * Created by richard.colvin on 29/11/2016.
 */
package meta

import validators.Validator
import java.lang.RuntimeException
import kotlin.reflect.KClass

// Types
interface Type {
  val name:String
}

data class AtomicType<T: Any>(
    val atomicType: KClass<T>
) : Type {
  override val name: String = atomicType.simpleName!!
}

data class ComplexType<E: Any, E_: Any> (
    val type_: KClass<E>,
    val type: KClass<E_>

) : Type {
  override val name: String = type.simpleName!!
}

data class UnionType (
    override val name:String,
    val types: List<Type>
) : Type


data class ListType<T: Type> (
    val listType: T
): Type {
  override val name: String = "List<${listType.name}>}"
}


//Fields
interface Field<T : Type> {
  val name: String
  val description: String
  val type: T
}

/**
 * Describes a Field that stores data a a given type.
 *  @param name the name of the field
 *  @param description describes the field
 *  @param fromString a function to convert a string to a value of the type: could throw an exception
 *  @param validate a function that validates that the given storage value is valid for the given type.If not a description of the error is returned else null if it is good. For example a File must exist/
 *  @param stringValidator a function that validates whether a given string can be converted into the storage type. If not a description is returned else null if it is good.
 */
data class AtomicField<T: Any>(
    override val type: AtomicType<T>,
    override val name: String,
    override val description: String,
    val validator: Validator<T>
) : Field<AtomicType<T>>


data class ComplexField<E: Any, E_: Any>(
    override val type: ComplexType<E, E_>,
    override val name: String,
    override val description: String
) : Field<ComplexType<E, E_>>


data class ListField<T: Type>(
    override val type: T,
    override val name: String,
    override val description: String

): Field<T>



interface FieldHolder<T: Type, out F: Field<T>> {
  val field: F
}

interface Fields<FH : FieldHolder<*, *>> {
  val all_: List<FH>
}


//
data class EntityField<X, X_, T : Type, E, E_, out F : Field<T>>(
    override val field: F,
    val get: (E) -> X,
    val get_: (E_) -> X_,
    val set_: (E_, X_) -> Unit,
    val nullable: Boolean = false
) : FieldHolder<T, F>


data class EntityMeta<E, E_>(
    val name: String,
    val fields: Fields<EntityField<*,*,*, E, E_, *>>,
    val builderFactory: () -> E_,
    val builder2Entity: (E_) -> E,
    val entity2Builder: (E) -> E_
)

class JsonException(msg: String) : RuntimeException(msg)
typealias JsonInt = Int
typealias JsonString = String
typealias JsonMap = Map<String, Any?>

/**
 * Maps a defined field between an entity and map that contains just JSON types.
 */
interface JsonMapper<E, E_, T:Type, F:Field<T>> : FieldHolder<T, F> {
  fun toJson(entity: E, map: MutableMap<String, Any?>)
  fun fromJson(map: Map<String, Any?>, entityBuilder: E_)
}

class JsonAtomicMapper<E, E_, T : Any, S : Any>(
    val entityField: EntityField<T, T?, AtomicType<T>, E, E_, AtomicField<T>>,
    val fromJson: (S) -> T,
    val toJson: (T) -> S
) : JsonMapper<E, E_,AtomicType<T> , AtomicField<T>>,
    FieldHolder<AtomicType<T>, AtomicField<T>> by entityField {

  override fun toJson(entity: E, map: MutableMap<String, Any?>) {

    val v: T = entityField.get(entity)
    with (field) {
      val validator: Validator<T> = validator
      if (!validator.test(v)) {
        throw JsonException("${name} ${validator.msg}")
      }
      map.put(name, toJson(v))
    }
  }


  override fun fromJson(
      map: Map<String, Any?>,
      entityBuilder: E_
  ) {
    val v = map[field.name] as S?
    if (v != null) {
      val set_ = entityField.set_ as (E_?, T) -> Unit
      set_(
          entityBuilder,
          fromJson(v)
      )
    } else {
      if (entityField.nullable) {
        entityField.set_(
            entityBuilder,
            null
        )
      } else {
        throw JsonException("")
      }
    }
  }
}

class JsonComplexMapper<E, E_, X: Any, X_:Any> (
    val entityField: EntityField<X, X?, ComplexType<X, X_>, E, E_, ComplexField<X,X_>>,
    val toJson: (X) -> Map<String, Any?>,
    val fromJson: (Map<String, Any?>) -> X
) : JsonMapper<E, E_,ComplexType<X, X_> , ComplexField<X,X_>>,
    FieldHolder<ComplexType<X, X_>, ComplexField<X,X_>> by entityField {
  override fun toJson(entity: E, map: MutableMap<String, Any?>) {
    val v = entityField.get(entity)
    map[entityField.field.name] = toJson(v)
  }

  override fun fromJson(
      map: Map<String, Any?>,
      entityBuilder: E_
  ) {
    val vMap = map[entityField.field.name] as Map<String, Any?>
    val x = fromJson(vMap)
    entityField.set_(entityBuilder, x)
  }

}



data class JsonMeta<E, E_>(
    val name: String,
    val fields: Fields<JsonMapper<E, E_, *, *>>
)

interface EntityParser<E, E_> {
  fun parse(text: String): E
  fun unparse(entity: E): String
}

/**
 * Json Parser that uses a intermediate Map
 *
 * Probably could be composed somehow if the functions were split out
 * Json->Map then Map->Entity
 * Entity->Map then Map->Json
 */
class JsonEntityParser<E, E_>(
    private val entityMeta: EntityMeta<E, E_>,
    private val jsonMeta: JsonMeta<E, E_>,
    private val jsonToMap: (String) -> Map<String, Any?>,
    private val mapToJson: (Map<String, Any?>) -> String
) : EntityParser<E, E_> {


  override fun parse(text: String): E {
    val map = jsonToMap(text)
    val b = entityMeta.builderFactory()

    jsonMeta.fields.all_.forEach {
      it.fromJson(map, b)
    }
    return entityMeta.builder2Entity(b)
  }

  override fun unparse(entity: E): String {
    val mm: MutableMap<String, Any?> = mutableMapOf()

    jsonMeta.fields.all_.forEach {
      it.toJson(entity, mm)
    }

    return mapToJson(mm)
  }
}

fun <E, E_> toJsonMap(
    jsonMeta: JsonMeta<E, E_>,
    entity:E
) : Map<String, Any?> {

  val mm: MutableMap<String, Any?> = mutableMapOf()

  jsonMeta.fields.all_.forEach {
    it.toJson(entity, mm)
  }
  return mm.toMap()
}

fun <E, E_> fromJsonMap(
    jsonMeta: JsonMeta<E, E_>,
    entityMeta: EntityMeta<E, E_>,
    map: Map<String, Any?>
) : E {
  val b = entityMeta.builderFactory()

  jsonMeta.fields.all_.forEach {
    it.fromJson(map, b)
  }
  return entityMeta.builder2Entity(b)
}


class DBMapper


//


fun stringField(
    name: String,
    description: String,
    validator: Validator<String>
) = AtomicField<String>(
    type = AtomicType(String::class),
    name = name,
    description = description,
    validator = validator
)

fun intField(
    name: String,
    description: String,
    validator: Validator<Int>
) = AtomicField<Int>(
    type = AtomicType(Int::class),
    name = name,
    description = description,
    validator = validator
)




/*
fun fileFieldMeta(
    name: String,
    description: String,
    validator: (File?) -> String? = { null }
): FieldMeta<File> =
    FieldMeta<File>(
        name = name,
        description = description,
        fromString = ::File,
        stringValidator = ::notEmptyString,
        validate = validator
    )

fun stringListFieldMeta(
    name: String,
    description: String,
    validator: (List<String>?) -> String? = { null }
): FieldMeta<List<String>> =
    FieldMeta<List<String>>(
        name = name,
        description = description,
        fromString = { it.split(",".toRegex()).map { it.trim() } },
        stringValidator = { null },
        validate = validator
    )

fun <T : Enum<T>> enumFieldMeta(
    name: String,
    description: String,
    appendDescription: Boolean = true,
    kclass: KClass<T>
): FieldMeta<T> =
    FieldMeta<T>(
        name = name,
        description = description + if (appendDescription) " " + kclass.enumValues().map { it.name }
            .joinToString() else "",
        fromString = { kclass.enumValueOf(it) },
        stringValidator = { null },
        validate = { null }
    )

*/



fun <E, E_> jsonStringMapper(
    field: EntityField<String, String?, AtomicType<String>, E, E_, AtomicField<String>>
): JsonMapper<E, E_,  AtomicType<String>, AtomicField<String>> =
    JsonAtomicMapper(field, { it }, { it })


fun <E, E_> jsonIntMapper(
    field: EntityField<Int, Int?, AtomicType<Int>, E, E_, AtomicField<Int>>
): JsonMapper<E, E_,  AtomicType<Int>, AtomicField<Int>> =
    JsonAtomicMapper(field, { it }, { it })

