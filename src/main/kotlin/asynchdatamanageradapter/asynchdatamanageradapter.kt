package asynchdatamanageradapter

import asynchdatamanager.AsyncDataManager
import datamanager.QueryDef
import datamanager.Sort
import java.util.concurrent.CompletableFuture
import datamanager.DataManager as SynchDataManager

/**
 * Created by richard.colvin on 19/06/2017.
 */
/**
 * Converts a synchronous AsyncDataManager into an Async version by applying
 * a strategy to create CompletableFutures
 */
class AsyncDataManagerAdapter<T, ID>(
    val synchDataManager: SynchDataManager<T, ID>,
    val cfFactory: (()->Any) -> CompletableFuture<Any>

) : AsyncDataManager<T, ID> {
  override fun query(
      queryDef: QueryDef,
      sort: Sort
  ): CompletableFuture<Sequence<T>> =
      async {
        synchDataManager.query(queryDef, sort)
      }

  override fun queryOne(queryDef: QueryDef): CompletableFuture<T> =
      async {
        synchDataManager.queryOne(queryDef)
      }

  override fun insert(t: T): CompletableFuture<T> =
      async {
        synchDataManager.insert(t)
      }

  override fun update(id: ID, t: T): CompletableFuture<T> =
      async {
        synchDataManager.update(id, t)
      }

  override fun delete(id: ID): CompletableFuture<ID> =
      async{
        synchDataManager.delete(id)
      }

  override fun id(id: ID): CompletableFuture<T> =
    async{
      synchDataManager.id(id)
    }


  private fun <X> async(f:()->X) : CompletableFuture<X> =
     (cfFactory as (()->X) -> CompletableFuture<X>)(f)


}