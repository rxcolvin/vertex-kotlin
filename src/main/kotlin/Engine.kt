import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.Vertx.vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.kotlin.core.VertxOptions
import rx.Observable
import rx.Observable.error
import rx.Observable.just
import rx.lang.kotlin.onError
import utils.getOrThrow
import utils.loadProperties
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by richard.colvin on 21/12/2016.
 */


fun main(args: Array<String>) {

  val vertx = vertx(VertxOptions().setBlockedThreadCheckInterval(999999L))

  val personDataManager = InMemoryDataManager<Person, String>(
      Person::uuid
  )

  val inserted = personDataManager.insert(
      Person(
          uuid = "1",
          firstName = "John",
          surname = "Doe"
      )
  ).toBlocking().first()

  val engine = VertxHttpServerEngine<Request, Response>(
      name = "REST Server",
      vertx = vertx,
      config = HttpEngineConfig(),
      buildRequest = ::buildRequest,
      buildResponse = ::buildResponse,
      requestRouterConfigs = listOf(
          FullRestAdapter<Context, Person, String>(
              assetType = "person",
              dataManager = personDataManager,
              string2Asset = ::stringToPerson,
              buildContext = ::requestToContext,
              asset2String = ::personToString,
              string2Id = { it },
              exception2Resp = ::exceptionToResponse
          )
      )
  )


  engine.start()
}



fun stringToPerson(s: String): Person {

  val map = loadProperties(s)
  return Person(
      uuid = map.getOrDefault("uuid", ""),
      firstName = map.getOrDefault("firstName", ""),
      surname = map.getOrDefault("surname", "")
  )
}

fun requestToContext(request: Request): Context = Context("1212")

fun personToString(person: Person): String =
    "uuid=${person.uuid},firstName=${person.firstName},surname=${person.surname}"

fun exceptionToResponse(exception: Exception) =
    Response(
        content = exception.toString()
    )


fun buildRequest(httpReq: HttpServerRequest) : Request {
  var content = ""
  httpReq.bodyHandler({ content = String(it.bytes) })
  return Request(
      action = methodToAction(httpReq.method()),
      path = httpReq.path().split("/".toRegex()).filter { it.isNotEmpty() },
      content = content
  )
}


fun buildResponse(resp: Response, httpResp: HttpServerResponse) {
  httpResp.end(resp.content)
}

fun printMultiMap(m: MultiMap) {
  m.names().forEach {
    println(" " + it + "=" + m.getAll(it).joinToString())
  }
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


data class Person(
    val uuid: String,
    val firstName: String,
    val surname: String
)

data class Context(val callerIp: String)

data class HttpEngineConfig(
    val port: Int = 8080,
    val host: String = "localhost"
)


interface Manageable {
  fun start()
  fun stop()
  // monitoring() - aide memoir
}

interface RequestRouter<REQ, RESP> {
  fun isDefinedFor(request: REQ): Handler<RESP>?

  interface Handler<RESP> {
    fun invoke() : RESP
  }

  interface NoBodyHandler<RESP>  : Handler <RESP>{
  }

  interface BodyHandler<RESP>  : Handler <RESP>{
    fun body(text:String) : RESP
  }

  interface LargeBodyHandler<RESP>  : Handler <RESP>{
    fun bodyPart(text: String)
  }


}

fun httpServerOptions(
    port: Int
) = HttpServerOptions().setPort(port)

/**
 *  This is a server engine that manages http requests using vertx.
 *
 *  It is general and just needs to know how to convert an vertx req in to a app REQ
 *  and a app RESP into a vertex resp.
 *
 *  This way the actual engine can be replaced by any other technology.
 *
 *  NB it should contain no real logic; it should never throw an exception of its own.
 *
 *  TODO: some way of streaming large responses
 *  TOOD: handle dynamic queries/subscriptions
 *  TODO: logging
 */
class VertxHttpServerEngine<REQ, RESP>(
    vertx: Vertx,
    buildRequest: (HttpServerRequest) -> REQ,
    buildResponse: (RESP, HttpServerResponse) -> Unit,
    val name: String,
    private val config: HttpEngineConfig,
    private val requestRouterConfigs: List<RequestRouter<REQ, RESP>>
) : Manageable {
  val server = vertx.createHttpServer(
      httpServerOptions(port = config.port)
  )

  init {
    server.requestHandler {
      val req = buildRequest(it)
      var handled = false
      for (rhc in requestRouterConfigs) {
        val h = rhc.isDefinedFor(req)
        if ( h != null) {
          when (h) {
            is RequestRouter.NoBodyHandler<*> -> {}
            is RequestRouter.BodyHandler<*> ->  {
              it.bodyHandler {
                  h.body(String(it.bytes))
              }
            }
            is RequestRouter.LargeBodyHandler<*> -> {

            }
          }
          buildResponse(h.invoke(), it.response())
          handled = true
          break
        }
      }
      if (!handled) {
        it.response().setStatusCode(400).end("UNKNOWN")
      }
    }
  }

  override fun start() {
    server.listen(
        config.port,
        config.host
    ) {
      if (it.succeeded()) {
        println("$name is Running")
      } else {
        println("$name Failed to start: " + it.cause())
      }

    }
  }

  override fun stop() {
    server.close {
      if (it.succeeded()) {
        println("Stopped")
      }
    }
  }


}

enum class Action {
  GET, PUT, POST, DELETE, OPTIONS, PATCH, HEAD, TRACE, CONNECT, OTHER
}

/**
 * Embryonic Representation of a general request
 */
data class Request(
    val action: Action,
    val path: List<String>,
    val content: String
)

/**
 * Embryonic Representation of an Application Response
 */
data class Response(
    val status: Int = 200,
    val content: String = "OK"
)


/**
 *
 */
class FullRestAdapter<C, T, ID>(
    val assetType: String,
    val buildContext: (Request) -> C,
    val string2Asset: (String) -> T,
    val asset2String: (T) -> String,
    val exception2Resp: (Exception) -> Response,
    val dataManager: DataManager<T, ID>,
    val string2Id: (String) -> ID
) : RequestRouter<Request, Response> {


  override fun isDefinedFor(request: Request): RequestRouter.Handler<Response>? =
      if (request.path[0] == assetType && (request.action == Action.GET || request.action == Action.POST)) {
        null
      } else {
        null
      }

  override fun invoke(it: Request):  =
      try {
        when (it.action) {
          Action.GET -> get(it)
          Action.PUT -> todo(it)
          Action.POST -> insert(it)
          Action.DELETE -> todo(it)
          Action.OPTIONS -> todo(it)
          Action.PATCH -> todo(it)
          Action.HEAD -> todo(it)
          Action.TRACE -> todo(it)
          Action.CONNECT -> todo(it)
          Action.OTHER -> todo(it)
        }
      } catch (e: Exception) {
        exception2Resp(e)
      }


  private fun get(request: Request): Response {
    val id = string2Id(request.path[1])
    val t = dataManager.id(id).onError { throw it }.toBlocking().first()
    return Response(
        content = asset2String(t)
    )
  }

  private fun insert(request: Request): Response {
    val t = dataManager.insert(string2Asset(request.content)).onError { throw it }.toBlocking().first()
    return Response(
        content = asset2String(t)
    )
  }

  private fun todo(request: Request): Response = Response(
      content = "TODO",
      status = 400
  )


}


// -- data manager
interface QueryDef {

}

data class IdQueryDef<ID>(
    val id: ID
) : QueryDef


interface DataQuery<T, ID> {
  fun query(queryDef: QueryDef): Observable<T>
  fun queryOne(queryDef: QueryDef): Observable<T>
  fun id(id: ID): Observable<T> = queryOne(IdQueryDef<ID>(id))
}

interface Subscription<T, ID> {
  val queryDef: QueryDef
  val onUpdate: (id: ID, t: T) -> Unit
  val onRemoved: (ID) -> Unit
  val onInserted: (id: ID, t: T) -> Unit
}

interface DataSubscription<T, ID> {
  fun subscribe(query: QueryDef): Subscription<T, ID>
}

interface DataManager<T, ID> : DataQuery<T, ID> {
  fun insert(t: T): Observable<T>
  fun update(id: ID, t: T): Observable<T>
  fun delete(id: ID): Observable<ID>
}

//- Data Manager shit

/**
 * A DataManager that stores things in Memory
 */
class InMemoryDataManager<T, ID>(
    val keyFunc: (T) -> ID
) : DataManager<T, ID> {
  val store = ConcurrentHashMap<ID, T>()

  override fun query(queryDef: QueryDef): Observable<T> {
    return error<T>(RuntimeException("TODO"))
  }

  override fun id(id: ID): Observable<T> {
    val item = store.get(id)
    if (item != null) {
      return just(item)
    }
    return error<T>(RuntimeException("Not Found: $id"))
  }

  override fun queryOne(queryDef: QueryDef): Observable<T> {
    return error<T>(RuntimeException("TODO"))
  }

  override fun insert(t: T): Observable<T> {
    store[keyFunc(t)] = t
    return just(t)
  }

  override fun update(id: ID, t: T): Observable<T> {
    return error<T>(RuntimeException("TODO"))
  }

  override fun delete(id: ID): Observable<ID> {
    store.remove(id)
    return just(id)
  }

}





