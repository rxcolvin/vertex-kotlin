package storage

import com.sun.xml.internal.bind.v2.model.core.ID
import kotlin.reflect.KClass


interface Storage {
  fun <T: Any, ID> put(id: ID, data: T)
  fun <ID> remove(id: ID)
  fun <T: Any, ID> getAll(klass: KClass<T>) : Map<ID, T>
}

interface TypedStorage<T: Any, ID> {
  fun put(id: ID, data: T)
  fun remove(id: ID)
  fun getAll() : Map<ID, T>
}
