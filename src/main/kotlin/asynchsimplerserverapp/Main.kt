package asynchsimplerserverapp

import adapters.AsyncChainAdapterChainAdapter
import adapters.AsyncLoggingAdapter
import asyncvertxhttpserverengine.HttpEngineConfig
import asyncvertxhttpserverengine.VertxCfFactory
import asyncvertxhttpserverengine.VertxHttpServerEngine
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import http.Action
import http.Request
import http.Response
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.kotlin.core.VertxOptions
import logging.Logger
import utils.Manageable
import utils.loadProperties
import java.io.File
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


data class Config(
    val storageBaseDir: File,
    val host: String = "localhost",
    val port: Int = 8080,
    val loggerDebug: Boolean = false
)

class ConfigException(s: String) : Exception(s)


fun config(map: Map<String, String>): Config =
    Config(
        storageBaseDir = File(map.getOrElse("storageBaseDir", { throw ConfigException("storageBaseDir property is missing") })),
        host = map.getOrElse("host", {"localhost"}),
        port = map.getOrElse("port", {"8080"}).toInt(),
        loggerDebug = map.getOrElse("loggerDebug", {"false"}).toBoolean()
    )


fun main(args: Array<String>) {

  try {

    val config = config(loadProperties(args))
    val logger = Logger(
        name = "Logger",
        debugEnabled = config.loggerDebug
    )

    val vertx = Vertx.vertx(
        VertxOptions().setBlockedThreadCheckInterval(10000)
    )

//    val gson =
//        GsonBuilder()
//            .registerTypeAdapter(LocalDate::class.java, LocalDateDeSerializer())
//            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
//            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeSerializer())
//            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
//            .create()

//    val competitionDataManager = InMemoryDataManager(
//        keyFunc = Competition::competitionId,
//        storage = FileStorage<Competition, String>(
//            directory = File(config.storageBaseDir, "competitions"),
//            keyToString = { it },
//            stringToKey = { it },
//            entityToString = gson::toJson,
//            stringToEntity = { gson.fromJson(it, Competition::class.java) },
//            fileExtension = "json"
//        ),
//        latencyEmulation = { 5000 }
//    )


    fun useAsync(): Manageable {
      val vertxCfFactory = VertxCfFactory(vertx)::completableFuture
      return VertxHttpServerEngine<Request, Response>(
          name = "A Server",
          vertx = vertx,
          config = HttpEngineConfig(
              host = config.host,
              port = config.port
          ),
          buildRequest = ::buildRequest,
          buildResponse = ::buildResponse,
          logger = logger,
          processor = AsyncChainAdapterChainAdapter<Request, Response>(
              arrayOf(
//                  AsyncFullRestAdapter<Request, Response, Context, Competition, String>(
//                      assetType = "competition",
//                      dataManager = AsyncDataManagerAdapter(
//                          synchDataManager = competitionDataManager,
//                          cfFactory = vertxCfFactory
//                      ),
//                      string2Asset = { gson.fromJson(it, Competition::class.java) },
//                      buildContext = ::requestToContext,
//                      asset2String = gson::toJson,
//                      idResolver = { it.path[1] },
//                      queryDefResolver = { QueryAll },
//                      assets2String = { gson.toJson(it.toList()) },
//                      getResolver = { it.action == Action.GET && it.path.size == 2 },
//                      assetNameResolver = { it.path[0] },
//                      queryResolver = { it.action == Action.GET && it.path.size == 1 },
//                      postResolver = { it.action == Action.POST && it.path.size == 2 },
//                      respFactory = { Response(200, it) },
//                      assetIdResolver = Competition::competitionId
//                  ),
                  AsyncLoggingAdapter(
                      path = "transmission",
                      pathResolver = { it.path.joinToString(separator = "/") },
                      actionResolver = { it.action.name },
                      logger = logger,
                      respFactory = { Response(200, "{\"status\":\"OK\"}") }
                  )
              )
          )
      )
    }
    useAsync().start()
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

class LocalDateDeSerializer : JsonDeserializer<LocalDate> {
  override fun deserialize(
      json: JsonElement?,
      typeOfT: Type?, context: JsonDeserializationContext?
  ): LocalDate {
    val s = json!!.getAsJsonPrimitive().asString
    return LocalDate.parse(s)
  }
}

class LocalDateTimeDeSerializer : JsonDeserializer<LocalDateTime> {
  override fun deserialize(
      json: JsonElement?,
      typeOfT: Type?,
      context: JsonDeserializationContext?
  ): LocalDateTime {
    val s = json!!.getAsJsonPrimitive().asString
    return LocalDateTime.parse(s)
  }
}

class LocalDateAdapter : JsonSerializer<LocalDate> {
  override fun serialize(
      date: LocalDate,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement {
    return JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE)) // "yyyy-mm-dd"
  }
}

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime> {
  override fun serialize(
      date: LocalDateTime,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement {
    return JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  }
}


fun requestToContext(request: Request): Context = Context("1212")

fun exceptionToResponse(exception: Exception) =
    Response(
        content = exception.toString()
    )

fun buildRequest(httpReq: HttpServerRequest): Request {
  return Request(
      action = methodToAction(httpReq.method()),
      path = httpReq.path().split("/".toRegex()).filter { it.isNotEmpty() }.toTypedArray(),
      params = httpReq.params().toMap()
  )
}


fun buildResponse(resp: Response, httpResp: HttpServerResponse) {
  httpResp.end(resp.content)
}

fun MultiMap.asString() =
    this.names().joinToString(separator = "\n") {
      " " + it + "=" + this.getAll(it).joinToString()
    }


fun methodToAction(m: HttpMethod): Action =
    when (m) {
      HttpMethod.OPTIONS -> Action.OPTIONS
      HttpMethod.GET -> Action.GET
      HttpMethod.HEAD -> Action.HEAD
      HttpMethod.POST -> Action.POST
      HttpMethod.PUT -> Action.PUT
      HttpMethod.DELETE -> Action.DELETE
      HttpMethod.TRACE -> Action.TRACE
      HttpMethod.CONNECT -> Action.CONNECT
      HttpMethod.PATCH -> Action.PATCH
      HttpMethod.OTHER -> Action.OTHER
    }


data class Context(val callerIp: String)

private fun MultiMap.toMap(): Map<String, Array<String>> =
    names().map { Pair(it, getAll(it).toTypedArray()) }.associate { it }

