package zztestapps

import entitymeta.EntityFieldMeta
import entitymeta.EntityMeta
import entitymeta.mapToEntity
import meta.intFieldMeta
import meta.stringFieldMeta
import validators.notEmptyString

/**
 * Created by richard.colvin on 24/03/2017.
 */

object dd {
  val name = stringFieldMeta(
      name = "name",
      description = "name desc",
      validator = ::notEmptyString
  )

  val age = intFieldMeta(
      name = "age",
      description = "age desc",
      validator = { if (it > 0) null else "must be positive" }
  )
}

object fooMeta : EntityMeta<Foo, Foo_> {
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

  override val entityName = "Foo"
  override val entityMetaFields = fields.all_
  override val builderFactory = ::Foo_
  override val builder2Entity = ::foo
  override val entity2Builder = ::foo_
  override val entityMetaFieldMap = fields.all_.associateBy { it.fieldMeta.name }
}


fun main(args: Array<String>) {
  val foo_ = fooMeta.builderFactory()
  fooMeta.fields.name.set_(foo_, "Bob")
  fooMeta.fields.age.set_(foo_, 34)
  val foo = fooMeta.builder2Entity(foo_)
  val foo2_ = fooMeta.entity2Builder(foo)

  println(fooMeta.fields.age.fieldMeta.type.stringValidator("_f"))
  println(fooMeta.entityMetaFieldMap["age"]!!.fieldMeta.validateAny(23))

  fooMeta.entityMetaFieldMap["age"]!!.setAny(foo2_, 23)

  val dataMap = mapOf(
      "name" to "Fred",
      "age" to "50"
  )

  println(mapToEntity(dataMap,  fooMeta))
}

data class Foo(
    val name: String,
    val age: Int

)

class Foo_(
) {
  var name: String? = null
  var age: Int? = null

}

fun foo(b: Foo_): Foo =
    Foo(
        b.name!!,
        b.age!!
    )


fun foo_(f: Foo) = Foo_().apply {
  this.name = f.name
}