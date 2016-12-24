/**
 * Created by richard.colvin on 29/11/2016.
 */
package meta

import logging.Logger
import utils.enumValueOf
import utils.enumValues
import utils.firstOrThrow
import utils.quotize
import validators.notEmptyString
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.*


interface  Type<T> {
    val name: String
    fun fromString(v:String): T
    fun valString(v:String) : Boolean
    val format: String
}

class ListType<T, TT:Type<T>>(
        val elementType:TT,
        override val name: String

) : Type<List<T>> {
    override fun fromString(v: String): List<T> {
        return v.split(",".toRegex()).map {elementType.fromString(it)}
    }

    override fun valString(v: String): Boolean {
        return !(false in v.split(",".toRegex()).map {elementType.valString(it)})
    }

    override val format = "csv string"
}

object StringType : Type<String> {
    override val name = "String"

    override fun valString(v: String) = true

    override fun fromString(v: String) =  v

    override val format = ""

}

val  StringListType = ListType<String, StringType> (StringType, "Strings")



interface Validator<T> {
    fun validate(v:T) : Boolean
    val help: String
}

data class FieldMeta1<T>(
        val type: Type<T>,
        val name: String,
        val description: String,
        val validator: Validator<T>
)




data class FieldMeta<T>(
        val name: String,
        val description: String,
        val fromString: (String) -> T,
        val validator: (T?) -> String? = { null },
        val stringValidator: (String) -> String? = { null }
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
        val fromString = fieldMeta.fromString(v)
        set_(e_, fromString)
    }

    fun validate(e_: E_): String? = fieldMeta.validator(get_(e_))

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
}

var logger:Logger = Logger("Meta")



fun stringFieldMeta(
        name: String,
        description: String,
        stringValidator: (String) -> String? = ::notEmptyString,
        validator: (String?) -> String? = {null}
): FieldMeta<String> =
        FieldMeta<String>(
                name = name,
                description = description,
                fromString = {it},
                stringValidator = stringValidator,
                validator = validator
        )

fun fileFieldMeta(
        name: String,
        description: String,
        validator: (File?) -> String? = {null}
): FieldMeta<File> =
        FieldMeta<File>(
                name = name,
                description = description,
                fromString = ::File,
                stringValidator = ::notEmptyString,
                validator = validator
        )

fun stringListFieldMeta(
        name: String,
        description: String,
        validator: (List<String>?) -> String? = {null}
): FieldMeta<List<String>> =
        FieldMeta<List<String>>(
                name = name,
                description = description,
                fromString = { it.split(",".toRegex()).map { it.trim() } },
                stringValidator = {null},
                validator = validator
        )

fun <T: Enum<T>> enumFieldMeta(
        name: String,
        description: String,
        appendDescription: Boolean = true,
        kclass: KClass<T>
        ): FieldMeta<T> =
        FieldMeta<T>(
                name = name,
                description = description +  if (appendDescription) " " + kclass.enumValues().map { it.name }
                        .joinToString() else "",
                fromString = {kclass.enumValueOf(it)},
                stringValidator = {null},
                validator = {null}
        )


fun <E : Any, E_ : Any> mapToEntity(map: Map<String, String>, em: EntityMeta<E, E_>): E {

    val builder = em.builderFactory()

    em.entityMetaFieldMap.forEach {

       logger.debug { "mapToEntity: processing entityMetaFieldMap entry: $it" }

        val (key, emf) = it
        val v = map[key]
        if (v != null) {
            val msg = emf.fieldMeta.stringValidator(v)
            if (msg != null) {
                throw ConfigurationException("Field $it validation error: $msg")
            }
            emf.fromString(builder, v)
        } else {
            if (!it.value.nullable && emf.get_(builder) == null) {
                throw  ConfigurationException("A value for property ${quotize(emf.fieldMeta.name)} must be provided")

            }
           logger.debug { it.key }
        }
    }

    val errors = em.entityMetaFieldMap.entries.map {
        Pair(it.key, it.value.validate(builder))
    }
            .filter {
                it.second != null
            }
            .map {
                "Property ${it.first} is invalid: ${it.second}"
            }

    if (errors.isNotEmpty()) {
        throw ConfigurationException(errors.joinToString { it + "\n" })
    }
    return em.builderFunction(builder)

}

class ConfigurationException(msg: String) : Exception(msg)

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


    val nameMeta
            = FieldMeta<String>(
            "name",
            "name desc",
            { it }
    )


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




