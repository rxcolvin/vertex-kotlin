package zztestapps

import entitymeta.EntityFieldMeta
import entitymeta.EntityJSONHelper
import entitymeta.EntityMeta
import io.vertx.core.json.JsonObject
import meta.intFieldMeta
import meta.stringFieldMeta
import validators.notEmptyString
import validators.positiveInt
import java.math.BigDecimal

/**
 * Created by richard.colvin on 24/03/2017.
 */

object dd {
  val name = stringFieldMeta(
      name = "name",
      description = "name desc",
      validator = notEmptyString
  )

  val age = intFieldMeta(
      name = "age",
      description = "age desc",
      validator = positiveInt
  )
}

object fooMeta {
  object fields {
    val name = EntityFieldMeta<String, Foo, Foo_>(
        fieldMeta = dd.name,
        get = { it.name },
        get_ = { it.name },
        set_ = { it, v -> it.name = v }
    )

    val age = EntityFieldMeta<Int, Foo, Foo_>(
        fieldMeta = dd.age,
        get = { it.age },
        get_ = { it.age },
        set_ = { it, v -> it.age = v }
    )

    val all_ = listOf(
        fields.name,
        fields.age
    )
  }

  val em = EntityMeta<Foo, Foo_>(
      entityName = "Foo",
      entityMetaFields = fields.all_,
      builderFactory = {Foo_(null, null)},
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
}


  fun main(args: Array<String>) {


    val foo_ = fooMeta.em.builderFactory()
    fooMeta.fields.name.set_(foo_, "Bob")
    fooMeta.fields.age.set_(foo_, 34)
    val foo = fooMeta.em.builder2Entity(foo_)
    val foo2_ = fooMeta.em.entity2Builder(foo)

//    println(fooMeta.fields.age.fieldMeta.type.stringValidator("_f"))
//    println(fooMeta.em.entityMetaFieldMap["age"]!!.fieldMeta.validateAny(23))

    fooMeta.em.entityMetaFieldMap["age"]!!.setAny(foo2_, 23)

    val dataMap = mapOf(
        "name" to "Fred",
        "age" to 50,
        "density" to 1.6,
        "bignum" to BigDecimal("123456556565656565656565656565656564677654567898765.8765456789998"),
        "mapping" to mapOf("x" to "yes", "z" to 45),
        "list" to listOf("foo", "bar")
    )

    val jsonObject = JsonObject(dataMap)

    println(jsonObject)

    jsonObject.map.forEach { (k, v) ->
      println("$k=$v:${v.javaClass}")
    }

    val jsonString = jsonObject.encodePrettily()

    println(jsonString)

    val jsonObjectAgain = JsonObject(jsonString)

    println("Again")
    jsonObjectAgain.map.forEach { (k, v) ->
      println("$k=$v:${v.javaClass}")
    }

    val jsonHelper = EntityJSONHelper<Foo, Foo_>(
        em = fooMeta.em,
        jsonToMap = { JsonObject(it).map },
        mapToJson = { JsonObject(it).encodePrettily() }
    )

    val tt = jsonHelper.fromJson(
        """
      |{
      |  "name": "Fred",
      |  "age": 56
      |}
      """.trimMargin()
    )

    println(tt)

    val jsonAgain = jsonHelper.toJson(tt)

    println(jsonAgain)

  }

  data class Foo(
      val name: String,
      val age: Int

  )

  data class Foo_(

    var name: String? = null,
    var age: Int? = null

  )



  fun foo_(f: Foo) = Foo_().apply {
    this.name = f.name
  }