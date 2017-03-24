package inmemorydatamanager

import datamanager.DataManager
import datamanager.QueryDef
import rx.Observable
import rx.Observable.just
import rx.Observable.error
import java.util.concurrent.ConcurrentHashMap

/**
 * A DataManager that stores things in Memory
 */
class InMemoryDataManager<T, ID>(
    val keyFunc: (T) -> ID
) : DataManager<T, ID> {
  val store = ConcurrentHashMap<ID, T>()

  override fun query(queryDef: QueryDef): Observable<T> {
    return error<T>(RuntimeException("TODO"))
  }

  override fun id(id: ID): Observable<T> {
    val item = store.get(id)
    if (item != null) {
      return just(item)
    }
    return error<T>(RuntimeException("Not Found: $id"))
  }

  override fun queryOne(queryDef: QueryDef): Observable<T> {
    return error<T>(RuntimeException("TODO"))
  }

  override fun insert(t: T): Observable<T> {
    store[keyFunc(t)] = t
    return just(t)
  }

  override fun update(id: ID, t: T): Observable<T> {
    return error<T>(RuntimeException("TODO"))
  }

  override fun delete(id: ID): Observable<ID> {
    store.remove(id)
    return just(id)
  }

}