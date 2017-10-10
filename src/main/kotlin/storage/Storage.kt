package storage

import kotlin.reflect.KClass


interface Storage {
  fun <T: Any, ID> put(id: ID, data: T)
  fun <ID> remove(id: ID)
  fun <T: Any, ID> getAll(klass: KClass<T>) : Map<ID, T>
}

