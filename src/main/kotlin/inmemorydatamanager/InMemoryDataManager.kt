package inmemorydatamanager

import com.sun.xml.internal.bind.v2.model.core.ID
import datamanager.DataManager
import datamanager.QueryAll
import datamanager.QueryDef
import datamanager.Sort
import datamanager.TypedDataManager
import logging.Logger
import storage.Storage
import storage.TypedStorage
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * A AsyncDataManager that stores things in Memory
 */
class InMemoryDataManager<T: Any, ID>(
    val logger: Logger = Logger("InMemoryDataManager_Default", debugEnabled = true),
    val storage: TypedStorage<T, ID>,
    val latencyEmulation: (() -> Long)? = null
) : TypedDataManager<T, ID> {
  val store = ConcurrentHashMap<ID, T>()

  override fun query(
      queryDef: QueryDef<T>,
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

  override fun  queryOne(queryDef: QueryDef<T>): T {
    throw RuntimeException("TODO")
  }

  override fun insert(id: ID, t: T): T {
    storage.put(id, t)
    return t
  }

  override fun update(id: ID, t: T): T {
    storage.put(id, t)
    return t
  }

  override fun delete(id: ID): ID {
    store.remove(id)
    return id
  }

}