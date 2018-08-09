package meta3

import com.sun.xml.internal.bind.v2.model.core.ID
import utils.getOrThrow
import kotlin.reflect.KClass


interface DbFieldMeta {

}


interface EntityMetaHolder<E : Any> {
  val kclass: KClass<E>
}

interface DbEntityMetaHolder<E : Any> : EntityMetaHolder<E> {
  fun get(index: Int): DbFieldMeta
}

interface UIMetaHolder<E : Any> : EntityMetaHolder<E> {

}

class EntityDao<E : Any>
(
    val dbMetaHolder: DbEntityMetaHolder<E>
) {

}


interface Type {
  val typeName: String
}

interface AtomicType<T : Any> : Type {
  val tType: KClass<T>
}

class FStringType(
    val length: Int
) : AtomicType<String> {
  override val typeName = "FString-" + length
  override val tType = String::class
}

object StringType
  : AtomicType<String> {
  override val typeName = "String"
  override val tType = String::class
}

object IntType : AtomicType<Int> {
  override val tType = Int::class
  override val typeName = "Int"
}

interface Field {
  val fieldName: String
}

class AtomicField<T : Any>(
    override val fieldName: String,
    val atomicType: AtomicType<T>
) : Field

open class EntityType(
    override val typeName: String,
    val fields: List<AtomicField<*>>
) : Type


data class Person(
    val name: String,
    val age: Int
)

val nameField = AtomicField("name", StringType)
val ageField = AtomicField("age", IntType)

object PersonType : EntityType(
    typeName = "Person",
    fields = listOf(
        nameField,
        ageField
    )
)

interface QueryDef

interface DbMapper<T, ID> {
  fun updateSchema()
  fun getById(id: ID): T
  fun put(item: T)
  fun query(queryDef: QueryDef): List<T>
}


interface Tuple {
  val arity: Int
}

data class T1<X> (
  val v1: X
): Tuple {
  override val arity = 1
}

data class DbColumn (
    val colName: String
)

interface AtomicTypeDbMapper {
  fun dbColumns(fieldName: String) : List<DbColumn>
}



class JdbcDbMapper<T, ID>(
    val entityType: EntityType,
    val map: Map<KClass<out AtomicType<out Any>>, AtomicTypeDbMapper>
) : DbMapper<T, ID> {

  override fun updateSchema() {
    println("Table Name=${entityType.typeName}")
    entityType.fields.forEach {
       map.get(it.atomicType::class)?.dbColumns(it.fieldName)?.forEach {
        println(it.colName)
      }
    }

  }

  override fun getById(id: ID): T {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun put(item: T) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun query(queryDef: QueryDef): List<T> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}

object StringTypeDbMapper : AtomicTypeDbMapper {
  override fun dbColumns(fieldName: String): List<DbColumn> {
    return listOf(
        DbColumn(fieldName)
    )
  }

}

object IntTypeDbMapper : AtomicTypeDbMapper {
  override fun dbColumns(fieldName: String): List<DbColumn> {
    return listOf(
        DbColumn(fieldName)
    )
  }

}

fun main(args: Array<String>) {
  val dbMapper = JdbcDbMapper<Person, String> (
      PersonType,
      mapOf(
          StringType::class to StringTypeDbMapper,
          IntType::class to IntTypeDbMapper
      )
  )

  dbMapper.updateSchema()
}

