package testmeta

import io.vertx.core.json.JsonObject
import meta.*
import validators.Validator
import validators.notEmptyString


object dd {
  val name = stringField(
      name = "name",
      description = "name desc",
      validator = notEmptyString
  )

  val age = intField(
      name = "age",
      description = "age desc",
      validator = Validator("must be a positive number") { it > 0 }
  )

  val location = stringField(
      name = "location",
      description = "location desc",
      validator = notEmptyString
  )


  val bar = ComplexField<Bar, Bar_> (
      type = barMeta.type,
      name = "bar",
      description = "bar desc"
  )

  val bars = ListField<ComplexType<Bar, Bar_>> (
      type = barMeta.type,
      name = "bars",
      description = "bars desc"
  )

  val cheeses = ListField<AtomicType<String>> (
      type = AtomicType<String>(String::class),
      name = "cheeses",
      description = "cheeses desc"
  )

  val hostelry = UnionField (
      type = UnionType(
          name = "hostelries",
          types = listOf(barMeta.type)

      ),
      name = "hostelry",
      description = "hostelry desc"
  )


}

data class Foo(
    val name: String,
    val age: Int,
    val bar: Bar,
    val bars: List<Bar>,
    val cheeses: List<String>
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
    var location:String?
) : Hostelry_

data class Pub (
    val name: String
) : Hostelry

data class Pub_ (
    var name: String?
) : Hostelry_

object fooMeta {
  open class FooFields<FH : FieldHolder<*, *>>(
      val name: FH,
      val age: FH,
      val bar: FH,
      val bars: FH,
      val cheeses: FH
  ) : Fields<FH> {
    override val all_ = listOf<FH>(name, age, bar, bars, cheeses)
  }

  val name = EntityField(
      field = dd.name,
      get = Foo::name::get,
      get_ = Foo_::name::get,
      set_ = Foo_::name::set
  )

  val age = EntityField(
      field = dd.age,
      get = Foo::age::get,
      get_ = Foo_::age::get,
      set_ = Foo_::age::set
  )

  val bar = EntityField(
      field = dd.bar,
      get = Foo::bar::get,
      get_ = Foo_::bar::get,
      set_ = Foo_::bar::set
  )

  val bars = EntityField(
      field = dd.bars,
      get = Foo::bars::get,
      get_ = Foo_::bars::get,
      set_ = Foo_::bars::set
  )

  val cheeses = EntityField(
      field = dd.cheeses,
      get = Foo::cheeses::get,
      get_ = Foo_::cheeses::get,
      set_ = Foo_::cheeses::set
  )


  val type = ComplexType<Foo, Foo_> (
      name = "Foo",
      builderFactory = { Foo_(null, null, bar = null, bars = null, cheeses = null) },
      builder2Entity = {
        Foo(
            it.name!!,
            it.age!!,
            Bar(
                location = it.bar!!.location!!
            )
            ,
            it.bars!!.map { Bar(it.location!!)},
            it.cheeses!!.toList()
        )
      },
      entity2Builder = {
        Foo_(
            name = it.name,
            age = it.age,
            bar = Bar_(it.bar.location),
            bars = it.bars.map{Bar_(
                location = it.location
            )}.toMutableList(),
            cheeses = it.cheeses.toMutableList()
        )
      }
  )

  val em = EntityMeta<Foo, Foo_>(
      name = "Foo",
      fields = FooFields(
          name = name,
          age = age,
          bar = bar,
          bars = bars,
          cheeses = cheeses
      ),
      type = type

   )


  val jm = JsonMeta<Foo, Foo_>(
      name = "foo",
      fields = FooFields(
          name = jsonStringFieldMapper(name),
          age = jsonIntFieldMapper(age),
          bar = JsonInvariantComplexFieldMapper(
              entityField = bar,
              jsonEntityMapper = barMeta.jsonFM

          ),
          bars = JsonInvariantComplexListFieldMapper(
              entityField = bars,
              jsonEntityMapper = barMeta.jsonFM
          ),
          cheeses = JsonInvariantAtomicListFieldMapper (
              entityField = cheeses,
              jsonMapper = jsonStringMapper
          )
      )
  )
}

object barMeta {
  val type = ComplexType<Bar, Bar_> (
      name = "Bar",
      builder2Entity = {
        Bar(
          location = it.location!!
      )
      },
      entity2Builder = {Bar_(it.location)},
      builderFactory = { Bar_(location = null) }
  )

  val location = EntityField(
      field = dd.location,
      get = Bar::location::get,
      get_ = Bar_::location::get,
      set_ = Bar_::location::set
  )

  open class BarFields<FH : FieldHolder<*, *>>(
      val location: FH
  ) : Fields<FH> {
    override val all_ = listOf<FH>(location)
  }


  val em = EntityMeta<Bar, Bar_>(
      name = "Bar",
      fields = BarFields(
          location = location
      ),
      type = type

  )

  val jm = JsonMeta<Bar, Bar_>(
      name = "bar",
      fields = BarFields(
          location = jsonStringFieldMapper(location)
      )
  )

  val jsonFM = JsonEntityMapper<Bar, Bar_> (
      entityMeta = em,
      jsonMeta = jm
 )

}

fun main(args: Array<String>) {

  val foo = Foo(
      name = "Joe",
      age = 101,
      bar = Bar("Fly")
      ,
      bars = listOf(
          Bar("Place 1"),
          Bar("Place 2")
      ),
      cheeses = listOf("Cheddar", "Edam")
  )


  val jsonParser = JsonEntityParser(
      entityMeta = fooMeta.em,
      jsonMeta = fooMeta.jm,
      jsonToMap = jsonToMap,
      mapToJson = mapToJson
  )
  println(jsonParser.unparse(foo))

  val foo2 = jsonParser.parse(
      """
        |{
        | "name" : "Joe",
        | "age" : 12,
        | "bar" : {
        |  "location": "High"
        |  },
        |  "bars" : [
        |  {
        |       "location" : "Hsdsds"
        |  } ,
        |  {
        |       "location" : "sdasd"
        |  }
        |  ],
        |  "cheeses" : ["Dairy-Lee", "Brie", "Stilton"]
         | }
    """.trimMargin())

  println(foo2)
}


//Should be in vertx specific package or refactored out by meta data driven json
val jsonToMap:  (String) -> Map<String, Any?> = { JsonObject(it).map }
val mapToJson: (Map<String, Any?>) -> String = { JsonObject(it).encodePrettily() }

