package logging

/*
 Simple Logging - avoid java logging bloatware; make use of Kotlin idioms

 Can be expanded if needed.

 TODO: add message formatting

 */
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import utils.GetOrDefault
import utils.GetOrDefaultImpl
import utils.pair
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction


private val errPw = PrintWriter(System.err)
private val outPw = PrintWriter(System.out)

class LogStream(
    val name: String,
    val ps: PrintWriter = errPw
) : Function1<Any, Unit> {

  override fun invoke(p1: Any) {
    if (p1 is Exception) {
      ps.println("[$name] ${p1.message}")
      p1.printStackTrace(ps)
    } else {
      ps.println("[$name] $p1")
      ps.flush()
    }
  }
}



class Logger(
    val name: String,
    val info_: ((Any) -> Unit) = ::println,
    val debug_: ((Any) -> Unit) = ::println,
    val error_: ((Any) -> Unit) = ::println,
    var infoEnabled: Boolean = true,
    var debugEnabled: Boolean = false
) {

  private val map = ConcurrentHashMap<KClass<*>, ClassLogger<*>>()
  private val fmap = ConcurrentHashMap<KFunction<*>, FunLogger>()

  inline fun info(s: () -> Any) {
    if (infoEnabled) info_(s())
  }

  inline fun debug(s: () -> Any) {
    if (debugEnabled) debug_(s())
  }

  inline fun error(s: () -> Any) {
    this.error_(s())
  }

  /**
   * Get a ClassLogger wrapper for this instance.
   */
  fun <T : Any> klogger(kclass: KClass<T>): ClassLogger<T> {
    return map.computeIfAbsent(
        kclass,
        { k -> ClassLogger(k, this) }
    ) as ClassLogger<T>

  }

  /**
   * Get a FunLogger wrapper for this instance.
   */
  fun flogger(kfun: KFunction<*>): FunLogger {
    return fmap.computeIfAbsent(
        kfun,
        { k -> FunLogger(k, this) }
    )

  }

}

/**
 * Log levels
 */
enum class LogLevel(val level: Int) {
  ERROR(-1), INFO(0), DEBUG(1)
}

data class LogStreamConfig(
    val output: String = "#:err"
)

data class LoggerConfig(
    val name: String,
    val level: LogLevel = LogLevel.INFO,
    val description: String = "",
    val infoConfig: LogStreamConfig = LogStreamConfig(),
    val debugConfig: LogStreamConfig = LogStreamConfig(),
    val errorConfig: LogStreamConfig = LogStreamConfig()
)

/**
 * class Logger: this wrapper includes a KClass that is used to get the name.
 */
class ClassLogger<T : Any>(
    val kclass: KClass<T>,
    val inner: Logger
) {
  private val map = ConcurrentHashMap<KFunction<*>, MethodLogger<*>>()

  inline fun info(s: () -> Any) {
    inner.info({ "${kclass.simpleName} ${s()}" })
  }

  inline fun debug(s: () -> Any) {
    inner.debug({ "[${kclass.simpleName}] ${s()}" })
  }

  inline fun error(s: () -> Any) {
    inner.error({  "[${kclass.simpleName}] ${s()}" })
  }

  fun mlogger(m: KFunction<*>): MethodLogger<T> {
    return map.computeIfAbsent(
        m,
        { k -> MethodLogger(k, this) }
    ) as MethodLogger<T>

  }
}

/**
 * Wraps a ClassLogger to give function specific logging
 */

class FunLogger(
    val kFunction: KFunction<*>,
    val inner: Logger
) {
  inline fun info(s: () -> Any) {
    inner.info({ "[${kFunction.name}] ${s()}" })
  }

  inline fun debug(s: () -> Any) {
    inner.debug({ "[${kFunction.name}] ${s()}" })
  }

  inline fun error(s: () -> Any) {
    inner.error({ "[${kFunction.name}] ${s()}" })
  }
}

/**
 * Wraps a class Logger to give method specific logging.
 */
class MethodLogger<T : Any>(
    val kFunction: KFunction<*>,
    val inner: ClassLogger<T>
) {
  inline fun info(s: () -> Any) {
    inner.inner.info({ "[${inner.kclass.simpleName}::${kFunction.name}] ${s()}" })
  }

  inline fun debug(s: () -> Any) {
    inner.inner.debug({ "[${inner.kclass.simpleName}::${kFunction.name}] ${s()}" })
  }

  inline fun error(s: () -> Any) {
    inner.inner.error({ "[${inner.kclass.simpleName}::${kFunction.name}] ${s()}" })
  }
}

/**
 * Logger of last resort.
 */
private val defoLogger = Logger("__Internal__")

/**
 * Creates a Get of degfault that is guarenteed to return a logger for any key
 * even if it is the logger of last resort. If a logger with the jey "__Default__"
 * is defined in the configuration this will be returned where no specific is defined
 * for the given key.
 */
fun loggerConfigsToLoggers(
    configs: List<LoggerConfig>
): GetOrDefault<String, Logger> {

  val map = configs.map {
    configToLogger(it)
  }.associateBy { it.name }

  val defo = map.getOrElse("__Default__", { defoLogger })

  return GetOrDefaultImpl(map, defo)
}




fun configToLogger(config: LoggerConfig): Logger =
    Logger(
        name = config.name,
        info_ = logStreamConfigToLogStream(LogLevel.INFO, config.infoConfig),
        debug_ = logStreamConfigToLogStream(LogLevel.DEBUG, config.debugConfig),
        error_ = logStreamConfigToLogStream(LogLevel.ERROR, config.errorConfig),
        debugEnabled = (config.level.level >= LogLevel.DEBUG.level),
        infoEnabled = (config.level.level >= LogLevel.INFO.level)

    )

/* Cache PrintWriters for a given absolute file names. */
private val printWriterCache = ConcurrentHashMap<String, PrintWriter>()

/**
 * Convert a LogStreamConfig to a LogStream
 */
private fun logStreamConfigToLogStream(
    name: LogLevel,
    config: LogStreamConfig
): LogStream {

  /* Any failed configs will fallback to this handling */
  fun handleDefault(msg: String): PrintWriter {
    val ret = errPw
    ret.println(msg)
    ret.println("Falling back to System.err")
    return ret;
  }

  /* Handle standard streams*/
  fun handleSystem(param: String): PrintWriter {
    return when (param) {
      "err" -> errPw
      "out" -> outPw
      else -> handleDefault("Bad output Config #:$param")
    }
  }

  /* Handle a File Stream */
  fun handleFile(fileName: String): PrintWriter {
    val ret = printWriterCache.computeIfAbsent(fileName, { PrintWriter(File(fileName)) })
    try {
      ret.println("")
    } catch (e: Exception) {
      return handleDefault("Can't write to file: $fileName because ${e.message}")
    }
    return ret
  }

  /* Currently two types of stream - could add others, like a queue*/
  fun stringToPrintWriter(output: String): PrintWriter {
    val (type, params) = output.pair()
    return when (type) {
      "#" -> handleSystem(params)
      "file" -> handleFile(params)
      else -> handleDefault("Bad output Config: $output")
    }
  }

  val ret = LogStream(
      name.name,
      stringToPrintWriter(config.output)
  )

  return ret;
}

fun loggerConfiFromFile(
    file: File
): List<LoggerConfig> {
  val mapper = jacksonObjectMapper()

  val json = file.readText()
  val x: Array<LoggerConfig> = mapper.readValue(json)

  return x.toList()

}

fun loadLoggers(fileName: String): GetOrDefault<String, Logger> {
  try {
    val configs = loggerConfiFromFile(File(fileName))
    return loggerConfigsToLoggers(configs)
  } catch (e: Exception) {
    System.err.println("Can't open logging config $fileName because ${e.message} - defaults will be used")
  }
  return loggerConfigsToLoggers(emptyList())
}

fun main(args: Array<String>) {
//    val configs = listOf(
//            LoggerConfig(
//                    name = "Foo",
//                    infoConfig = LogStreamConfig(
//                            output = "file:info.log"
//                    )
//            )
//    )

  val configs = loggerConfiFromFile(File("logging.json"))

  val logger = loggerConfigsToLoggers(configs)["Foo"]!!


  logger.info { "Hello" }
  logger.infoEnabled = false
  logger.debug { "Doh" }
  logger.debugEnabled = true
  logger.debug { "Doh Doh" }

}


