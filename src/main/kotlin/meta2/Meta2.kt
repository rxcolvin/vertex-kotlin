package meta2

import kotlin.reflect.KClass

sealed class Type<K : Any> {
  abstract val klass: KClass<K>
  abstract val uniqueName: String
}

data class AtomicType<K : Any>(
    override val uniqueName: String,
    override val klass: KClass<K>
) : Type<K>()

data class AtomicListType<K : Any>(
    override val uniqueName: String,
    val atomicKClass: KClass<K>
) : Type<AtomicList<K>>() {
  override val klass = AtomicList::class as KClass<AtomicList<K>>
}

data class ComplexListType<K : Any>(
    override val uniqueName: String,
    val baseKClass: KClass<K>,
    val types: List<Type<*>>
) : Type<ComplexList<K>>() {
  override val klass = ComplexList::class as KClass<ComplexList<K>>
}

data class FieldType<K : Any, CTK : Any>(
    val name: String,
    val type: Type<K>,
    val getter: (ct: CTK) -> K
)

data class ComplexType<K : Any>(
    override val uniqueName: String,
    override val klass: KClass<K>,
    val fieldMap: Map<String, FieldType<*, K>>
) : Type<K>()


interface TypeHolder<K : Any> {
  val type: Type<K>
}

class TypeResolver(
    types: List<Type<*>>,
    kclassResolvers: List<(Any) -> Type<*>?>
) {
  private val typeMap = types.associate { it.klass to it }
  val resolvers = kclassResolvers + { typeMap[it::class] } + {(it as? TypeHolder<*>)?.type }

  fun <K : Any> resolve(item: K): Type<K> =
      resolvers.map { it(item) }.filter {it != null}.firstOrNull() as Type<K>? ?: throw Exception(item.toString() + " " + item::class)
}

class JsonParser(
    private val typeResolver: TypeResolver
) {

  fun <K : Any> toJson(item: K?): String =
      item?.let {
        val type = typeResolver.resolve(it)
        println(item)
        println(type)
        when (type) {
          is AtomicType<*> -> toJson(it, type as AtomicType<K>)
          is ComplexType<*> -> toJson(it, type as ComplexType<K>)
          is AtomicListType<*> -> toJson(it, type as AtomicListType<K>)
          is ComplexListType<*> -> toJson(it, type as ComplexListType<K>)
        }
      } ?: "null"

  private fun <K : Any> toJson(item: K, type: AtomicType<K>): String =
      when (type.klass) {
        Int::class -> (item as Int).toString()
        Double::class -> (item as Double).toString()
        Float::class -> (item as Float).toString()
        Boolean::class -> (item as Boolean).toString()
        else -> quote(item.toString())
      }

  private fun <K : Any> toJson(item: K, type: ComplexType<K>): String {

    val ret = StringBuilder()
    ret.append("{")
    type.fieldMap.forEach {
      ret.append(quote(it.key))
      ret.append(" : ")
      ret.append(toJson(it.value.getter(item)))
    }
    ret.append("}")
    return ret.toString()
  }

  private fun <K : Any> toJson(item: K, type: AtomicListType<K>): String {
    val list = item as List<*>
    val ret = StringBuilder()
    ret.append("[")
    list.map { toJson(it) }.joinTo(ret, separator = ", ")
    ret.append("]")
    return ret.toString()
  }

  private fun <K : Any> toJson(item: K, type: ComplexListType<K>): String {
    val list = item as List<*>
    val ret = StringBuilder()
    ret.append("[")
    ret.append("{\n")
    ret.append(quote("type"))
    ret.append(" : ")
    ret.append(quote(type.uniqueName))
    ret.append(",\n")
    ret.append(quote("value"))
    ret.append(" : ")
    list.map { toJson(it) }.joinTo(ret)
    ret.append("}")
    ret.append("]")
    return ret.toString()
  }

}


fun quote(s: String) = '"' + s + '"'


data class Foo(
    val name: String,
    val age: Int,
    val bar: Bar,
    val bars: AtomicList<Bar>,
    val cheeses: AtomicList<String>,
    val randoms: ComplexList<Any>
)

data class Foo_(
    var name: String?,
    var age: Int?,
    var bar: Bar_?,
    var bars: MutableList<Bar_>?,
    var cheeses: MutableList<String>?

)

interface Hostelry
interface Hostelry_

data class Bar(
    val location: String
) : Hostelry

data class Bar_(
    var location: String?
) : Hostelry_

data class Pub(
    val name: String
) : Hostelry

data class Pub_(
    var name: String?
) : Hostelry_

fun main(args: Array<String>) {
  val atomicIntType = AtomicType("int", Int::class)
  val atomicStringType = AtomicType("string", String::class)
  val barType = ComplexType(
      "Bar",
      Bar::class,
      mapOf()
  )
  val randomsListType = ComplexListType("RandomList", Any::class, listOf(atomicStringType, atomicIntType, barType))
  val barListType = AtomicListType("List<Bar>", Bar::class)
  val stringListType = AtomicListType("List<String>", String::class)

  val fooType = ComplexType(
      "Foo",
      Foo::class,
      listOf<FieldType<*, Foo>>(
          FieldType(name = "name", type = atomicStringType, getter = { it.name }),
          FieldType(name = "age", type = atomicIntType, getter = { it.age }),
          FieldType(name = "bar", type = barType, getter = { it.bar }),
          FieldType(name = "bars", type = barListType, getter = { it.bars }),
          FieldType(name = "cheeses", type = stringListType, getter = { it.cheeses }),
          FieldType(
              name = "randoms",
              type = randomsListType,
              getter = { it.randoms }
          )
      ).associate { it.name to it }
  )

  val typeResolver = TypeResolver(
      types = listOf(atomicIntType, atomicStringType, barType, fooType),
      kclassResolvers = listOf()
  )

  typeResolver.resolve(atomicListOf(stringListType))

  val jsonParser = JsonParser(
      typeResolver
  )

  val foo = Foo(
      name = "foo 1",
      age = 23,
      bar = Bar(location = "Here"),
      bars = atomicListOf(barListType, Bar(location = "There")),
      cheeses = atomicListOf(stringListType, "edam", "cheddar"),
      randoms = complexListOf(randomsListType, "string", 23, Bar(location = "None"))
  )

  println(jsonParser.toJson(foo))
}

class AtomicList<E : Any>(
    override val type: AtomicListType<E>,
    val delegate: List<E>
) : List<E> by delegate, TypeHolder<AtomicList<E>> {
  override fun toString(): String {
    return delegate.toString()
  }
}

fun <E : Any> atomicListOf(type: AtomicListType<E>, vararg elements: E) = AtomicList(type, elements.asList())

class ComplexList<E : Any>(
    override val type: ComplexListType<E>,
    val delegate: List<E>
) : List<E> by delegate, TypeHolder<ComplexList<E>> {
  override fun toString(): String {
    return delegate.toString()
  }

}

fun <E : Any> complexListOf(type: ComplexListType<E>, vararg elements: E) = ComplexList(type, elements.asList())
