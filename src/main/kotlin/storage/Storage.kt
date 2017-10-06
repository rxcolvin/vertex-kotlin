package storage


interface Storage<T, ID> {
  fun put(id: ID, data: T)
  fun remove(id: ID)

  fun getAll() : Map<ID, T>
}

