package utils

import io.vertx.core.MultiMap
import java.io.File
import java.lang.Exception
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import java.lang.Enum as JavaLangEnum


/**
 * Load a property map from a file
 */
fun loadProperties(file: File): Map<String, String> =
        file.readLines()
                .map { it.trim() }
                .filter { !it.startsWith("#") }
                .map { it.split("=".toRegex(), 2) }
                .map { Pair(it[0].trim(), it[1].trim()) }
                .associate { it }

/**
 * Load a property map from an array
 */
fun loadProperties(args: Array<String>) : Map<String, String>{
    return args
           .map {it.split("=".toRegex(), 2) }
            .map { Pair(it[0], it.getOrElse(1, {""})) }
            .associate { it }
}

fun loadProperties(s: String, sep: String = ",") : Map<String, String> {
    return loadProperties(s.split(sep.toRegex()).toTypedArray())
}


/**
 * Merge one map into target an create a new one.
 */
fun <K, V> Map<K, V>.merge(other: Map<K, V>): Map<K, V> {
    val result = LinkedHashMap<K, V>(this)
    result.putAll(other)
    return result
}


fun String.pair(sep: String = ":") : Pair<String, String> {
    val ss = this.split(sep.toRegex(), 2)
    return Pair(ss[0], if (ss.size == 2)  ss[1] else "")
}


fun <K, V ,E: Exception> Map<K,V>.getOrThrow( key: K, f: (K) -> E ) : V {
    val v = this.get(key)
    if (v == null) {
        throw f(key)
    }
    return v
}

fun <T> identity(x: T): T = x

inline fun <T> Iterable<T>.firstOrThrow(predicate: (T) -> Boolean, e: ()->Exception): T {
    for (element in this) if (predicate(element)) return element
    throw e()
}


fun <T> Array<T>.tail() : Array<T> {
    val ret = this.sliceArray(kotlin.ranges.IntRange(1, this.size - 1))
    return ret
}

fun <T>  Array<T>.headAndTail() : Pair<T, Array<T>> =
        Pair(this.first(), this.tail())


interface GetOrDefault<K, V> {
    operator fun get(k: K): V
}

class GetOrDefaultImpl<K, V> (
        private val map: Map<K, V>,
        private val defo: V
) : GetOrDefault<K, V> {

    override operator fun get(k: K): V {
        return map.getOrElse(k, {defo})
    }
}

class ProgramingException(msg: String) : Exception(msg){

}

inline fun quotize(s: String) : String {
    return '"' + s + '"'
}


fun <T : Enum<T>> KClass<T>.enumValues(): Array<T> = java.enumConstants

fun <T : Enum<T>> KClass<T>.enumValueOf(name: String): T = JavaLangEnum.valueOf(java, name)




enum class State {
    NONE, NAME, PARAMETERS
}


fun <K,V> getFrom(k:K, vararg maps: Map<K, V>) : V? {

    for (m in maps) {
        val ret = m[k]
        if (ret != null) {
            return ret
        }
    }
    return null
}


interface Manageable {
  fun start()
  fun stop()
  // monitoring() - aide memoir
}

/**
 *
 */
inline fun <reified FROM : Any, reified TO> convert(from: FROM): TO {
    val con = TO::class.constructors.first()
    val fromType = FROM::class
    val params = con.parameters.map {
        val name = it.name
        Pair(con.parameters.first { it.name == name },
            fromType.declaredMemberProperties.first { it.name == name }.get(from))

    }.associate { it }

    println(params)
    return TO::class.constructors.first().callBy(params)

}

//Converts some object into another one.
fun <T, R> T.into(f: T.() -> R) = f(this)



data class TimeIt<R> (
    val duration: Long,
    val result: R?,
    val exception: Throwable?
)

fun <R> timeIt(f: () -> R) : TimeIt<R> {

  val time = LocalDateTime.now()
  try {

    val res = f()
    return TimeIt(ChronoUnit.MILLIS.between(time, LocalDateTime.now()), res, null)
  } catch (t: Throwable) {
    return TimeIt(ChronoUnit.MILLIS.between(time, LocalDateTime.now()), null, t)

  }
}

fun MultiMap.asString() =
    this.names().joinToString(separator = "\n") {
      " " + it + "=" + this.getAll(it).joinToString()
    }
