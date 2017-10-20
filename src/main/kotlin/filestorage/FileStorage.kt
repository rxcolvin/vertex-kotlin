package filestorage

import storage.Storage
import storage.TypedStorage
import java.io.File
import java.io.FileFilter
import java.util.concurrent.ConcurrentHashMap

class TypedFileStorage<T:Any, ID>(
    val directory: File,
    val keyToString: (ID) -> String,
    val stringToKey: (String) -> ID,
    val entityToString: (T) -> String,
    val stringToEntity: (String) -> T,
    val fileExtension: String
) : TypedStorage<T, ID> {

  val lockMap: Map<ID, ID> = ConcurrentHashMap()

  init {

    if (directory.exists() && !directory.isDirectory) {
      throw RuntimeException("File not dir") //TODO
    }

    if (!directory.exists()) {
      if (!directory.mkdirs()) {
        throw RuntimeException("Cant create Dir") //TODO
      }
    }
  }


  override fun put(
      id: ID,
      data: T
  ) {
    val lock = lockMap.getOrElse(id, { id })
    synchronized(lock as Any) {
      try {
        val file = File(directory, keyToString(id) + "." + fileExtension)
        file.createNewFile()
        file.writeText(entityToString(data))
      } finally {
        lockMap.minus(id)
      }
    }
  }

  override fun remove(id: ID) {
    val lock = lockMap.getOrElse(id, { id })
    synchronized(lock as Any) {
      try {
        val file = File(directory, keyToString(id) + "." + fileExtension)
        file.delete()
      } finally {
        lockMap.minus(id)
      }
    }
  }

  override fun getAll(): Map<ID, T> =
      directory.listFiles(FileFilter {
        it.isFile && it.extension == fileExtension
      }).map {
        Pair(
            stringToKey(it.name),
            stringToEntity(it.readText())
        )
      }.toMap()

}