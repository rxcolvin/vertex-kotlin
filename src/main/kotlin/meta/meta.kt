/**
 * Created by richard.colvin on 29/11/2016.
 */
package meta

import logging.Logger
import validators.notEmptyString
import javax.print.DocFlavor


// The logger for this module set to a default value: generally should be reset.
var logger: Logger = Logger(
    name = "meta",
    debugEnabled = true
)

enum class JSONType{
    STRING, NUMBER, LIST, MAP
}

data class Type<T>(
    val name: String,
    val fromString: (String) -> T,
    val toString: (T) -> String,
    val stringValidator: (String) -> String? = { null },
    val jsonType: JSONType = JSONType.STRING
)


val stringType = Type<String>(
    name = "string",
    fromString = { it },
    toString = { it },
    stringValidator = { null }
)

val intType = Type<Int>(
    name = "int",
    fromString = { it.toInt() },
    toString = { it.toString() },
    stringValidator = { if (it.toIntOrNull() != null) null else "Not a number" },
    jsonType = JSONType.NUMBER
)

/**
 * Describes a Field that stores data a a given type.
 *  @param name the name of the field
 *  @param description describes the field
 *  @param fromString a function to convert a string to a value of the type: could throw an exception
 *  @param validate a function that validates that the given storage value is valid for the given type.If not a description of the error is returned else null if it is good. For example a File must exist/
 *  @param stringValidator a function that validates whether a given string can be converted into the storage type. If not a description is returned else null if it is good.
 */
data class FieldMeta<T>(
    val name: String,
    val type: Type<T>,
    val description: String,
    val validate: (T) -> String? = { null }
) {
  /**
   *
   */
  fun validateAny(t: Any) = validate(t as T)
}


fun stringFieldMeta(
    name: String,
    description: String,
    validator: (String) -> String? = { null }
) = FieldMeta<String>(
    name = name,
    description = description,
    type = stringType,
    validate = validator
)

fun intFieldMeta(
    name: String,
    description: String,
    validator: (Int) -> String? = { null }
) = FieldMeta<Int>(
    name = name,
    description = description,
    type = intType,
    validate = validator
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







