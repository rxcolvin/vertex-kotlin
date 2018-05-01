package meta3

import kotlin.reflect.KClass


interface  Validator<K: Any?> {
  fun test(t: K): Boolean
  fun msg(t: K) : String
}

object DefaultValidator: Validator<Any?> {
  override fun test(t: Any?) = true
  override fun msg(t: Any?) = ""
}

interface  Type<K: Any> {
  val name:String
  val required: Boolean

}



data class AtomicType<K : Any> (
    override val name:String,
    val kclass: KClass<K>,
    val fromString: (String)->K,
    val validate: Validator<K> = DefaultValidator as Validator<K>,
    val toString: (K) -> String = {it.toString()}
 ) : Type<K> {
  override val required = true
}

data class AtomicNullableType<K : Any> (
    override val name:String,
    val kclass: KClass<K>,
    val fromString: (String)->K?,
    val validate: Validator<K?> = DefaultValidator as Validator<K?>,
    val toString: (K?) -> String? = {it?.toString()}
) : Type<K>  {
  override val required = true

}



data class AtomicListType<K : Any> (
    override val name:String,
    val atomicType: AtomicType<K>
) : Type<List<K>> {
  override val required = true
}


data class ComplexListType<K: Any> (
    override val name:String,
    val array: Array<Type<*>>
) : Type<List<K>>  {
  override val required = true
}

open class FieldType<K: Any> (
    val name: String,
    val type: Type<K>
)

 class AtomicFieldType<K: Any> (
     name: String,
     type: AtomicType<K>

) : FieldType<K>  (
     name, type
)


data class ComplexType<K: Any> (
    override val name: String,
    val array: Array<FieldType<*>>
) : Type<K> {
  override val required = true
}

val atomicString = AtomicType<String>(
    name = "String",
    kclass = String::class,
    fromString = {it}
)

val atomicString_ = AtomicNullableType<String>(
    name = "String?",
    kclass = String::class,
    fromString = {it}
)


val atomicInt = AtomicType<Int>(
    name = "Int",
    kclass = Int::class,
    fromString = {it.toInt()}
)

val name = AtomicFieldType<String>(
  "name", atomicString
)

val age = AtomicFieldType<Int> (
    name  = "age",
    type = atomicInt
)


data class Person(
    val name: String
)

val person = ComplexType<Person>(
    "Person",
    arrayOf(
        name, age
    )
)

//Entity Meta


class EntityField<E, E_, K: Any> (
    val fieldType: Type<K>,
    val getter: (E_) -> K
)

class EntityMeta<E, E_, K: Any> (
    val complexType: ComplexType<K>

)


//DbMeta
class DbColumn<K: Any>(
    val fieldType: AtomicFieldType<K>
)


//UIMeta







