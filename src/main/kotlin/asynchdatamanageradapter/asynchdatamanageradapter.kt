package asynchdatamanageradapter

import asynchdatamanager.AsyncDataManager
import com.sun.xml.internal.bind.v2.model.core.ID
import datamanager.QueryDef
import datamanager.Sort
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import datamanager.DataManager as SynchDataManager

/**
 * Created by richard.colvin on 19/06/2017.
 */
/**
 * Converts a synchronous AsyncDataManager into an Async version by applying
 * a strategy to create CompletableFutures
 */
class AsyncDataManagerAdapter(
    val synchDataManager: SynchDataManager,
    val cfFactory: (()->Any) -> CompletableFuture<Any>

) : AsyncDataManager {
  override fun <T: Any> query(
      queryDef: QueryDef<T>,
      sort: Sort
  ): CompletableFuture<Sequence<T>> =
      async {
        synchDataManager.query(queryDef, sort)
      }

  override fun <T: Any> queryOne(queryDef: QueryDef<T>): CompletableFuture<T> =
      async {
        synchDataManager.queryOne(queryDef)
      }

  override  fun <T: Any> insert(t: T): CompletableFuture<T> =
      async {
        synchDataManager.insert(t)
      }

  override fun <T:Any , ID> update(id: ID, t: T): CompletableFuture<T> =
      async {
        synchDataManager.update(id, t)
      }

  override fun <T: Any, ID> delete(
      id: ID,
      klass: KClass<T>
  ): CompletableFuture<ID> =
      async{
        synchDataManager.delete(id, klass)
      }

  override fun <T: Any, ID> id(id: ID, klass: KClass<T>): CompletableFuture<T> =
    async{
      synchDataManager.id(id, klass)
    }


  private fun <X> async(f:()->X) : CompletableFuture<X> =
     (cfFactory as (()->X) -> CompletableFuture<X>)(f)


}