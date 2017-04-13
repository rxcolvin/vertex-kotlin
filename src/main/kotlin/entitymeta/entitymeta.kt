package entitymeta

import logging.Logger
import meta.FieldMeta
import utils.firstOrThrow
import utils.quotize
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.memberProperties

var logger: Logger = Logger(
    name = "entitymeta",
    debugEnabled= true
)

interface FieldMetaHolder<T> {
   val fieldMeta: FieldMeta<T>
}

interface EntityField<T,E : Any, E_ : Any> : FieldMetaHolder<T> {
  val get: (E) -> T
  val get_: (E_) -> T?
  val set_: (E_, T?) -> Unit
  val nullable: Boolean
}

interface JsonField<T, S> :  FieldMetaHolder<T> {
  val fromJson: (S) -> T
  val toJson: (T) -> S
}

val a: FieldMetaHolder<String> = object : FieldMetaHolder<String>, JsonField<String, String>, EntityField<String, Any, Any> {
  override val fieldMeta: FieldMeta<String>
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val get: (Any) -> String
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val get_: (Any) -> String?
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val set_: (Any, String?) -> Unit
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val nullable: Boolean
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val fromJson: (String) -> String
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val toJson: (String) -> String
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

/**
 * For a given Entity Type <E> and associated Entity Builder Type (<E_>) this classe
 * associates a FieldMeta with some storage in those typs; defining how
 * the field value is accessed from both types and whether the value is mandatory.
 *
 * @param fieldMeta the given field meta name
 * @param get a Function to extract the field value from the Entity
 * @param get_ a Functrion to extract the field value from the Entity Builder
 * @param set_ a Function to inject the field value into the Entity Builder
 * @param nullable Defines whether a field is nullable with in the given Entity type
 *
 * (TODO: The mandatory-ness may be deduced from the field type perhaps?)
 */
class EntityFieldMeta<T : Any, E : Any, E_ : Any>(
    val fieldMeta: FieldMeta<T>,
    val get: (E) -> T,
    val get_: (E_) -> T?,
    val set_: (E_, T?) -> Unit,
    val nullable: Boolean = false
) {
  fun setAny(e:E_, v:Any?) : Unit {
    this.set_(e, v as T)
  }
  /**
   * A constructor that uses properties to define the setters and getters:
   * NB May be to move this to a function in a seperate package if not supported in JS.
   */

}

/**
 * A constructor that uses the KClass and looks up properties using the field name.
 * NB May be to move this to a function in a seperate package if not supported in JS.
 */
fun <T : Any, E : Any, E_ : Any> entityFieldMeta(
    fieldMeta: FieldMeta<T>,
    eType: KClass<E>,
    e_Type: KClass<E_>,
    nullable: Boolean = false
) = entityFieldMeta(
    fieldMeta,
    eType.memberProperties.firstOrThrow({ it.name == fieldMeta.name },
        { RuntimeException("${eType.simpleName} has no property ${fieldMeta.name}") }) as KProperty1<E, T>,
    e_Type.memberProperties.firstOrThrow({ it.name == fieldMeta.name },
        { RuntimeException("${e_Type.simpleName} has no property ${fieldMeta.name}") }) as KMutableProperty1<E_, T?>,
    nullable
)


fun <T : Any, E : Any, E_ : Any> entityFieldMeta(
    fieldMeta: FieldMeta<T>,
    epropType: KProperty1<E, T>,
    e_propType: KMutableProperty1<E_, T?>,
    nullable: Boolean = false

) = EntityFieldMeta(
    fieldMeta,
    epropType.getter,
    e_propType.getter,
    e_propType.setter,
    nullable
)


/**
 * A Simple wrapper around a [FieldMeta] with a nullable attribute that is just used to pass to
 * an [EntityMeta].
 */
data class EntityFieldMetaConfig<T>(
    val fieldMeta: FieldMeta<T>,
    val nullable: Boolean = false
)

/**
 * Define Meta data for a given entity type/entity builder type.
 *
 * @param name the name of the entity - must be unique within some context.
 * @param entityMetaFields a list of [EntityFieldMetaConfig] elements used to define the [EntityFieldMeta]s for this entity
 */
data class EntityMeta<E : Any, E_ : Any> (
    val entityName: String,
    val entityMetaFields: List<EntityFieldMeta<*, E, E_>>,
    val builderFactory: () -> E_,
    val builder2Entity: (E_) -> E,
    val entity2Builder: (E) -> E_
) {
  val entityMetaFieldMap: Map<String, EntityFieldMeta<*, E, E_>>
      = entityMetaFields.associateBy { it.fieldMeta.name }
}


//TODO: Move to a configuration package or something more general
fun <E : Any, E_ : Any> mapToEntity(map: Map<String, String>, em: EntityMeta<E, E_>): E {

  val builder = em.builderFactory()

  em.entityMetaFieldMap.forEach {

    logger.debug { "mapToEntity: processing entityMetaFieldMap entry: $it" }

    val (key, emf) = it
    val v = map[key]
    if (v != null) {
      val msg = emf.fieldMeta.type.stringValidator(v)
      if (msg != null) {
        throw ConfigurationException("Field $it validation error: $msg")
      }
      val ev = emf.fieldMeta.type.fromString(v)
      val msg2 = emf.fieldMeta.validateAny(ev)
      if (msg2 != null) {
        throw  ConfigurationException("Error in value for property ${quotize(emf.fieldMeta.name)} : " + msg2)
      }
      emf.setAny(builder, ev)
    } else {
      if (!it.value.nullable) {
        throw  ConfigurationException("A value for property ${quotize(emf.fieldMeta.name)} must be provided")

      }
      logger.debug { it.key }
    }
  }
  return em.builder2Entity(builder)

}

class ConfigurationException(msg: String) : Exception(msg)

fun <E : Any, E_ : Any> EntityMeta<E, E_>.fieldHelp(): String =
    this.entityMetaFieldMap.map {
      "${it.key} - ${it.value.fieldMeta.description}"
    }.joinToString("\n")