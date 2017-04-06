package simplerserverapp

import entitymeta.EntityFieldMeta
import entitymeta.EntityJSONHelper
import entitymeta.EntityMeta
import json.jsonToMap
import json.mapToJson

data class Person(
    val uuid: String,
    val firstName: String,
    val surname: String,
    val age: Int
)

data class Person_(
    var uuid: String?,
    var firstName: String?,
    var surname: String?,
    var age: Int?
) {
  constructor() : this(null, null, null, null)
}


object personMeta {

  val uuid = EntityFieldMeta<String, Person, Person_>(
      fieldMeta = dd.uuid,
      get = { it.uuid },
      get_ = { it.uuid },
      set_ = { it, v -> it.uuid = v }
  )

  val firstName = EntityFieldMeta<String, Person, Person_>(
      fieldMeta = dd.firstName,
      get = { it.firstName },
      get_ = { it.firstName },
      set_ = { it, v -> it.firstName = v }
  )

  val surname = EntityFieldMeta<String, Person, Person_>(
      fieldMeta = dd.surname,
      get = { it.surname },
      get_ = { it.surname },
      set_ = { it, v -> it.surname = v }
  )
  val age = EntityFieldMeta<Int, Person, Person_>(
      fieldMeta = dd.age,
      get = { it.age },
      get_ = { it.age },
      set_ = { it, v -> it.age = v }
  )

  val entityMeta = EntityMeta<Person, Person_>(
      entityName = "Person",
      entityMetaFields = listOf(
          uuid,
          firstName,
          surname,
          age
      ),
      builderFactory = { Person_() },
      builder2Entity = {
        Person(
            it.uuid as String,
            it.firstName as String,
            it.surname as String,
            it.age as Int
        )
      },
      entity2Builder = {
        Person_(
            it.uuid,
            it.firstName,
            it.surname,
            it.age
        ) }
  )

  val personJsonHelper = EntityJSONHelper<Person, Person_>(
      em = entityMeta,
      mapToJson = mapToJson,
      jsonToMap = jsonToMap
  )
}
