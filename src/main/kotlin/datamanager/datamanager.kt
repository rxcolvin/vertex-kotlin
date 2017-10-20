package datamanager

import kotlin.reflect.KClass


// -- data manager
interface QueryDef<T>

data class IdQueryDef<ID, T: Any>(
    val id: ID
) : QueryDef<T>

class QueryAll<T: Any> : QueryDef<T>

interface Sort {
  val isAscending: Boolean;
  val fields:Array<String>
}

object unordered : Sort {
  override val isAscending: Boolean = true
  override val fields: Array<String> = emptyArray()
}
//rx versions

interface DataQuery {
  fun <T: Any> query(
      queryDef: QueryDef<T>,
      sort: Sort = unordered
  ): Sequence<T>
  fun <T: Any> queryOne(queryDef: QueryDef<T>): T
  fun <T: Any, ID> id(id: ID, klass: KClass<T>): T = queryOne(IdQueryDef<ID, T>(id))
}

interface Subscription<T: Any, in ID> {
  val queryDef: QueryDef<T>
  val onUpdate: (id: ID, t: T) -> Unit
  val onRemoved: (ID) -> Unit
  val onInserted: (id: ID, t: T) -> Unit
}

interface DataSubscription {
  fun <T: Any, ID> subscribe(query: QueryDef<T>): Subscription<T, ID>
}

interface DataManager : DataQuery {
  fun <T: Any, ID> insert(id:ID, t: T): T
  fun <T: Any, ID> update(id: ID, t: T): T
  fun <T: Any, ID> delete(id: ID, kclass: KClass<T>): ID
}




interface TypedDataQuery<T: Any, ID> {
  fun  query(
      queryDef: QueryDef<T>,
      sort: Sort = unordered
  ): Sequence<T>
  fun  queryOne(queryDef: QueryDef<T>): T
  fun  id(id: ID): T = queryOne(IdQueryDef<ID, T>(id))
}

interface TypedDataManager<T: Any, ID> : TypedDataQuery<T, ID> {
  fun insert(id:ID, t: T): T
  fun update(id: ID, t: T): T
  fun delete(id: ID): ID
}



//- Data Manager shit