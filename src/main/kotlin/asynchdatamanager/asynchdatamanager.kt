package asynchdatamanager

import datamanager.IdQueryDef
import datamanager.QueryDef
import datamanager.Sort
import datamanager.unordered
import java.util.concurrent.CompletableFuture

/**
 * Created by richard.colvin on 19/06/2017.
 */



interface DataQuery<T, ID> {
  fun query(
      queryDef: QueryDef,
      sort: Sort = unordered
  ): CompletableFuture<Sequence<T>>
  fun queryOne(queryDef: QueryDef): CompletableFuture<T>
  fun id(id: ID): CompletableFuture<T> = queryOne(IdQueryDef<ID>(id))
}

interface Subscription<T, ID> {
  val queryDef: QueryDef
  val onUpdate: (id: ID, t: T) -> Unit
  val onRemoved: (ID) -> Unit
  val onInserted: (id: ID, t: T) -> Unit
}

interface DataSubscription<T, ID> {
  fun subscribe(query: QueryDef): Subscription<T, ID>
  fun unsubscribe(subscription: Subscription<T, ID>)
}

interface AsyncDataManager<T, ID> : DataQuery<T, ID> {
  fun insert(t: T): CompletableFuture<T>
  fun update(id: ID, t: T): CompletableFuture<T>
  fun delete(id: ID): CompletableFuture<ID>
}
