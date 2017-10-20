package asynchdatamanageradapter

import asynchdatamanager.AsyncDataManager
import asynchdatamanager.TypedAsyncDataManager
import com.sun.xml.internal.bind.v2.model.core.ID
import datamanager.QueryDef
import datamanager.Sort
import datamanager.TypedDataManager
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
class TypedAsyncDataManagerAdapter<T:Any, ID>(
    val synchDataManager: TypedDataManager<T, ID>,
    val cfFactory: (()->Any) -> CompletableFuture<Any>

) : TypedAsyncDataManager<T, ID> {
  override fun  query(
      queryDef: QueryDef<T>,
      sort: Sort
  ): CompletableFuture<Sequence<T>> =
      async {
        synchDataManager.query(queryDef, sort)
      }

  override fun queryOne(queryDef: QueryDef<T>): CompletableFuture<T> =
      async {
        synchDataManager.queryOne(queryDef)
      }

  override  fun insert(id: ID, t: T): CompletableFuture<T> =
      async {
        synchDataManager.insert(id, t)
      }

  override fun update(id: ID, t: T): CompletableFuture<T> =
      async {
        synchDataManager.update(id, t)
      }

  override fun  delete(
      id: ID
  ): CompletableFuture<ID> =
      async{
        synchDataManager.delete(id)
      }



  private fun <X> async(f:()->X) : CompletableFuture<X> =
     (cfFactory as (()->X) -> CompletableFuture<X>)(f)


}