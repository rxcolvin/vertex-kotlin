/**
 * Created by richard.colvin on 29/11/2016.
 */
package meta

import logging.ClassLogger
import logging.Logger
import logging.MethodLogger
import tuples.T2
import utils.enumValueOf
import utils.enumValues
import utils.firstOrThrow
import utils.quotize
import validators.notEmptyString
import java.io.File
import java.lang.Integer.parseInt
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.*


interface  Type<T> {
    val name: String
    fun fromString(v:String): T
    fun valString(v:String) : Boolean
    val help: String
}

class ListType<T, TT:Type<T>>(
        val elementType:TT,
        override val name: String

) : Type<List<T>> {
    override fun fromString(v: String): List<T> {
        return v.split(",".toRegex()).map {elementType.fromString(it)}
    }

    override fun valString(v: String): Boolean {
        return false !in v.split(",".toRegex()).map {elementType.valString(it)}
    }

    override val help = "[<value1>,..,<valueN>]"
}

object StringType : Type<String> {
    override val name = "String"

    override fun valString(v: String) = true

    override fun fromString(v: String) =  v

    override val help = ""

}

object IntType : Type<Int> {
    override val name = "Int"

    override fun valString(v: String) =
         try {
            parseInt(v)
            true
        }       catch (e: NumberFormatException) {
            false
        }


    override fun fromString(v: String) =  parseInt(v)

    override val help = "A integer Number between ${Int.MIN_VALUE} and ${Int.MAX_VALUE} "

}

val  StringListType = ListType<String, StringType> (StringType, "Strings")

val  IntListType = ListType<Int, IntType> (IntType, "Ints")



enum class ValidState {
    OK, WARNING, ERROR
}


data class Validatum (
        val state: ValidState,
        val msg: String = ""
)

data class FieldMeta<T>(
        val type: Type<T>,
        val name: String,
        val description: String,
        val validator: (T) -> Validatum
)






data class EntityFieldMetaConfig<T>(
        val fieldMeta: FieldMeta<T>,
        val nullable: Boolean = false

)

class EntityFieldMeta<T, E : Any, E_ : Any>(
        val fieldMeta: FieldMeta<T>,
        val get: (E) -> T,
        val get_: (E_) -> T?,
        val set_: (E_, T?) -> Unit,
        val nullable: Boolean = false
) {
    constructor(
            fieldMeta: FieldMeta<T>,
            epropType: KProperty1<E, T>,
            e_propType: KMutableProperty1<E_, T?>,
            nullable: Boolean = false

    ) :
            this(fieldMeta, epropType.getter, e_propType.getter, e_propType.setter, nullable) {
    }

    constructor(
            fieldMeta: FieldMeta<T>,
            eType: KClass<E>,
            e_Type: KClass<E_>,
            nullable: Boolean = false

    ) : this(
            fieldMeta,
            eType.memberProperties.firstOrThrow({ it.name == fieldMeta.name }, { RuntimeException("${eType.simpleName} has no property ${fieldMeta.name}") }) as KProperty1<E, T>,
            e_Type.memberProperties.firstOrThrow({ it.name == fieldMeta.name }, { RuntimeException("${e_Type.simpleName} has no property ${fieldMeta.name}") }) as KMutableProperty1<E_, T?>,
            nullable
    )


    fun fromString(e_: E_, v: String) {
        val fromString = fieldMeta.type.fromString(v)
        set_(e_, fromString)
    }

    fun validate(e_: E_): Validatum {
        val v = get_(e_)
        if (v != null) {
            return fieldMeta.validator(v)
        } else  {
            if (nullable) {
                return Validatum(ValidState.OK)
            } else {
                return Validatum(ValidState.ERROR, "${fieldMeta.name} must be set")
            }
        }
    }

}


class EntityMeta<E : Any, E_ : Any>(
        val name: String,
        entityMetaFields: List<EntityFieldMeta<*, E, E_>>,
        val builderFactory: () -> E_,
        val builderFunction: (E_) -> E

) {
    constructor(
            name: String,
            eType: KClass<E>,
            e_Type: KClass<E_>,
            fieldMetas: List<EntityFieldMetaConfig<*>>,
            factory_: () -> E_,
            factory: (E_) -> E

    ) : this(
            name,
            fieldMetas.map {
                EntityFieldMeta(
                        it.fieldMeta,
                        eType,
                        e_Type,
                        it.nullable
                )
            },
            factory_,
            factory
    )

    val entityMetaFieldMap = entityMetaFields.associateBy { it.fieldMeta.name }

    val clogger = ClassLogger(EntityMeta::class, logger)


    fun  mapToEntity(
            map: Map<String, String>
    ): Pair<E, List<String>> {

        val logger = MethodLogger(EntityMeta<E, E_>::mapToEntity, clogger)

        val builder = builderFactory()

        val used = map.keys.toMutableList()

        entityMetaFieldMap.forEach {

            logger.debug { "mapToEntity: processing entityMetaFieldMap entry: $it" }

            val (key, emf) = it
            val v = map[key]
            if (v != null) {
                val valid = emf.fieldMeta.type.valString(v)
                if (!valid) {
                    throw ConfigurationException("Error when parsing the string $it to a ${emf.fieldMeta.type.name} format should be: ${emf.fieldMeta.type.help}")
                }
                emf.fromString(builder, v)

            } else {
                if (!it.value.nullable) {
                    throw  ConfigurationException("A value for property ${quotize(emf.fieldMeta.name)} must be provided")

                }
            }
            used.remove(key)
        }

        validateEntityBuilder(builder)


        return Pair(builderFunction(builder), used.toList())

    }

    fun validateEntityBuilder(entityBuilder: E_){
        val errors = entityMetaFieldMap.entries.map {
            Pair(it.key, it.value.validate(entityBuilder))
        }.filter { it.second.state == ValidState.ERROR}

                .map {
                    "Property ${it.first} is invalid: ${it.second.msg}"
                }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors.joinToString { it + "\n" })
        }
    }

}

var logger:Logger = Logger("Meta")








class ConfigurationException(msg: String) : Exception(msg)
class ValidationException(msg: String) : Exception(msg)

data class Foo(
        val name: String
)

class Foo_(
) {
    var name: String? = null

}

fun foo(b: Foo_): Foo =
        Foo(b.name!!)


fun main(args: Array<String>) {


    val from = Foo_()
    from.name="Food"
    println(convert<Foo_,  Foo>(from))

    val fooMeta1 = EntityMeta(
            name = "Foo",
            e_Type = Foo_::class,
            eType = Foo::class,
            fieldMetas = listOf<EntityFieldMetaConfig<*>>(

            ),
            factory_ = ::Foo_,
            factory = ::foo
    )

    val foo_ = fooMeta1.builderFactory()

    fooMeta1.entityMetaFieldMap["name"]!!.fromString(foo_, "Hello")

    val foo = fooMeta1.builderFunction(foo_)





}

inline fun  <reified FROM: Any, reified TO>  convert(from: FROM) : TO {
    val con = TO::class.constructors.first()
    val fromType = FROM::class
    val params = con.parameters.map {
        val name = it.name
        Pair(con.parameters.first { it.name == name }, fromType.declaredMemberProperties.first { it.name == name }.get(from))

    }.associate { it }

    println(params)
    return TO::class.constructors.first().callBy(params)

}

fun <E: Any, E_: Any> EntityMeta<E, E_>.fieldHelp() : String =
    this.entityMetaFieldMap.map {
      "${it.key} - ${it.value.fieldMeta.description}"
    }.joinToString("\n")




