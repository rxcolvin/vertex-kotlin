package simplerserverapp

import meta.intFieldMeta
import validators.Validator
import validators.notEmptyString

/**
 * Created by richard.colvin on 06/04/2017.
 */
object dd {
  val uuid = meta.stringFieldMeta(
      name = "uuid",
      description = "uuid desc",
      validator = notEmptyString
  )

  val firstName = meta.stringFieldMeta(
      name = "firstName",
      description = "firstName desc",
      validator = notEmptyString
  )

  val surname = meta.stringFieldMeta(
      name = "surname",
      description = "surname desc",
      validator = notEmptyString
  )


  val age = intFieldMeta(
      name = "age",
      description = "age desc",
      validator = Validator("Must be a positive")  { it > 0}
  )
}
