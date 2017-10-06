package inmemorydatamanager

import datamanager.DataManager
import datamanager.QueryAll
import datamanager.QueryDef
import datamanager.Sort
import logging.Logger
import storage.Storage
import java.util.concurrent.ConcurrentHashMap

/**
 * A AsyncDataManager that stores things in Memory
 */
class InMemoryDataManager<T, ID>(
    val keyFunc: (T) -> ID,
    val logger: Logger = Logger("InMemoryDataManager_Default", debugEnabled = true),
    val storage: Storage<T, ID>,
    val latencyEmulation: (() -> Long)? = null
) : DataManager<T, ID> {
  val store = ConcurrentHashMap<ID, T>(storage.getAll())

  override fun query(
      queryDef: QueryDef,
      sort: Sort
  ): Sequence<T> {
    if (queryDef is QueryAll) {
      return store.values.asSequence()
    }
    throw RuntimeException("QueryDef Not Found: ${queryDef}")
  }

  override fun id(id: ID): T {
    latencyEmulation?.let {
      val millis = it()
      logger.debug { "LatencyEmulation for $millis ms on Thread: ${Thread.currentThread()}" }
      Thread.sleep(millis)
    }
    val item = store.get(id)
    if (item != null) {
      return item
    }
    throw  RuntimeException("Not Found: $id")
  }

  override fun queryOne(queryDef: QueryDef): T {
    throw RuntimeException("TODO")
  }

  override fun insert(t: T): T {
    val key = keyFunc(t)
    store[key] = t
    storage.put(key, t)
    return t
  }

  override fun update(id: ID, t: T): T {
    val key = keyFunc(t)
    store[key] = t
    storage.put(key, t)
    return t
  }

  override fun delete(id: ID): ID {
    store.remove(id)
    return id
  }

}