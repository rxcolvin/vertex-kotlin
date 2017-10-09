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
      type = barMeta.barType,
      name = "bar",
      description = "bar desc"
  )

  val bars = ListField<ComplexType<Bar, Bar_>> (
      type = barMeta.barType,
      name = "bars",
      description = "bars desc"
  )

}

data class Foo(
    val name: String,
    val age: Int,
    val bar: Bar,
    val bars: List<Bar>
)

data class Foo_(
    var name: String?,
    var age: Int?,
    var bar: Bar_?,
    var bars: MutableList<Bar_>?
)

data class Bar(
    val location: String
)

data class Bar_(
    var location:String?
)




object fooMeta {
  open class FooFields<FH : FieldHolder<*, *>>(
      val name: FH,
      val age: FH,
      val bar: FH,
      val bars: FH
  ) : Fields<FH> {
    override val all_ = listOf<FH>(name, age, bar, bars)
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


  val em = EntityMeta<Foo, Foo_>(
      name = "Foo",
      fields = FooFields(
          name = name,
          age = age,
          bar = bar,
          bars = bars
      ),
      builderFactory = { Foo_(null, null, bar = null, bars = null) },
      builder2Entity = {
        Foo(
            it.name!!,
            it.age!!,
            Bar(
                location = it.bar!!.location!!
            )
            ,
            it.bars!!.map { Bar(it.location!!)}
        )
      },
      entity2Builder = {
        Foo_(
            name = it.name,
            age = it.age,
            bar = Bar_(it.bar.location),
            bars = it.bars.map{Bar_(
                location = it.location
            )}.toMutableList()
        )
      }
  )


  val jm = JsonMeta<Foo, Foo_>(
      name = "foo",
      fields = FooFields(
          name = jsonStringMapper(name),
          age = jsonIntMapper(age),
          bar = JsonComplexFieldMapper<Foo, Foo_, Bar, Bar_>(
              entityField = bar,
              jsonEntityMapper = barMeta.jsonFM

          ),
          bars = JsonInvariantComplexListFieldMapper(
              entityField = bars,
              json
          )
      )
  )
}

object barMeta {
  val barType = ComplexType (
      Bar::class, Bar_::class
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
      builderFactory = { Bar_(null) },
      builder2Entity = {
        Bar(
            it.location!!
        )
      },
      entity2Builder = {
        Bar_(
            it.location
        )
      }
  )

  val jm = JsonMeta<Bar, Bar_>(
      name = "bar",
      fields = BarFields(
          location = jsonStringMapper(location)
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
      )
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
        |  ]
         | }
    """.trimMargin())

  println(foo2)
}


//Should be in vertx specific package or refactored out by meta data driven json
val jsonToMap:  (String) -> Map<String, Any?> = { JsonObject(it).map }
val mapToJson: (Map<String, Any?>) -> String = { JsonObject(it).encodePrettily() }

