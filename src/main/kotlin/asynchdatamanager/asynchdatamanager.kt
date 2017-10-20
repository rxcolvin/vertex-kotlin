package asynchdatamanager

import com.sun.xml.internal.bind.v2.model.core.ID
import datamanager.IdQueryDef
import datamanager.QueryDef
import datamanager.Sort
import datamanager.unordered
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * Created by richard.colvin on 19/06/2017.
 */



interface DataQuery {
  fun <T: Any> query(
      queryDef: QueryDef<T>,
      sort: Sort = unordered
  ): CompletableFuture<Sequence<T>>
  fun <T: Any> queryOne(queryDef: QueryDef<T>): CompletableFuture<T>
  fun <T: Any, ID>id(id: ID, klass: KClass<T>): CompletableFuture<T> = queryOne(IdQueryDef<ID, T>(id))
}



interface Subscription<T, ID> {
  val queryDef: QueryDef<T>
  val onUpdate: (id: ID, t: T) -> Unit
  val onRemoved: (ID) -> Unit
  val onInserted: (id: ID, t: T) -> Unit
}

interface DataSubscription {
  fun <T, ID> subscribe(query: QueryDef<T>): Subscription<T, ID>
  fun <T, ID> unsubscribe(subscription: Subscription<T, ID>)
}

interface DataChange {
  fun <T: Any, ID> insert(id: ID, t: T): CompletableFuture<T>
  fun <T: Any, ID> update(id: ID, t: T): CompletableFuture<T>
  fun <T: Any, ID> delete(id: ID, klass: KClass<T>): CompletableFuture<ID>
}

interface AsyncDataManager: DataChange, DataQuery {
}


interface Transaction {
  fun <T: Any, ID> insert(id: ID, t: T)
  fun <T: Any, ID> update(id: ID, t: T)
  fun <T: Any, ID> delete(id: ID, klass: KClass<T>)
  fun commit() : CompletableFuture<Unit>
  fun rollback(): CompletableFuture<Unit>
}

interface TransactionManager {
   fun createTx()
}




interface TypeDataQuery<T: Any, ID> {
  fun query(
      queryDef: QueryDef<T>,
      sort: Sort = unordered
  ): CompletableFuture<Sequence<T>>
  fun queryOne(queryDef: QueryDef<T>): CompletableFuture<T>
  fun id(id: ID): CompletableFuture<T> = queryOne(IdQueryDef<ID, T>(id))
}

interface TypedDataChange<T:Any, ID> {
  fun insert(id: ID, t: T): CompletableFuture<T>
  fun update(id: ID, t: T): CompletableFuture<T>
  fun delete(id: ID): CompletableFuture<ID>
}

interface TypedAsyncDataManager<T:Any, ID>: TypedDataChange<T, ID>, TypeDataQuery<T, ID> {
}




