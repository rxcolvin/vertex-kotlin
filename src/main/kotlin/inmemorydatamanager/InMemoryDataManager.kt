package inmemorydatamanager

import com.sun.xml.internal.bind.v2.model.core.ID
import datamanager.DataManager
import datamanager.QueryAll
import datamanager.QueryDef
import datamanager.Sort
import logging.Logger
import storage.Storage
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * A AsyncDataManager that stores things in Memory
 */
class InMemoryDataManager(
    val logger: Logger = Logger("InMemoryDataManager_Default", debugEnabled = true),
    val storage: Storage,
    val latencyEmulation: (() -> Long)? = null
) : DataManager {
  val store = ConcurrentHashMap<KClass<*>, ConcurrentHashMap<*,*>>()

  override fun <T: Any> query(
      queryDef: QueryDef<T>,
      sort: Sort
  ): Sequence<T> {
    if (queryDef is QueryAll) {
      return store.values.asSequence()
    }
    throw RuntimeException("QueryDef Not Found: ${queryDef}")
  }

  override fun <T: Any> id(id: ID): T {
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

  override fun <T: Any> queryOne(queryDef: QueryDef<T>): T {
    throw RuntimeException("TODO")
  }

  override fun <T:Any> insert(t: T): T {
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