package asynchsimplerserverapp


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


}
