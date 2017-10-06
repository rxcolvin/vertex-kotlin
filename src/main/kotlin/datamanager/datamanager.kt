package datamanager


// -- data manager
interface QueryDef {

}

data class IdQueryDef<ID>(
    val id: ID
) : QueryDef

object QueryAll : QueryDef

interface Sort {
  val isAscending: Boolean;
  val fields:Array<String>
}

object unordered : Sort {
  override val isAscending: Boolean = true
  override val fields: Array<String> = emptyArray()
}
//rx versions

interface DataQuery<T, ID> {
  fun query(
      queryDef: QueryDef,
      sort: Sort = unordered
  ): Sequence<T>
  fun queryOne(queryDef: QueryDef): T
  fun id(id: ID): T = queryOne(IdQueryDef<ID>(id))
}

interface Subscription<T, ID> {
  val queryDef: QueryDef
  val onUpdate: (id: ID, t: T) -> Unit
  val onRemoved: (ID) -> Unit
  val onInserted: (id: ID, t: T) -> Unit
}

interface DataSubscription<T, ID> {
  fun subscribe(query: QueryDef): Subscription<T, ID>
}

interface DataManager<T, ID> : DataQuery<T, ID> {
  fun insert(t: T): T
  fun update(id: ID, t: T): T
  fun delete(id: ID): ID
}


//- Data Manager shit