package datamanager

import rx.Observable

// -- data manager
interface QueryDef {

}

data class IdQueryDef<ID>(
    val id: ID
) : QueryDef


interface DataQuery<T, ID> {
  fun query(queryDef: QueryDef): Observable<T>
  fun queryOne(queryDef: QueryDef): Observable<T>
  fun id(id: ID): Observable<T> = queryOne(IdQueryDef<ID>(id))
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
  fun insert(t: T): Observable<T>
  fun update(id: ID, t: T): Observable<T>
  fun delete(id: ID): Observable<ID>
}

//- Data Manager shit