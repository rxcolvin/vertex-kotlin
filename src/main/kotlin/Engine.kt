import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.Vertx.vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import rx.Observable
import rx.Observable.error
import rx.Observable.just
import rx.lang.kotlin.onError
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by richard.colvin on 21/12/2016.
 */


fun main(args: Array<String>) {

  val vertx = vertx()
  val personDataManager = InMemoryDataManager<Person, String>(
      Person::name
  )

  val inserted = personDataManager.insert(Person("ddfd")).toBlocking().first()

  val engine = HttpEngine<Request, Response>(
      name = "REST Server",
      vertx = vertx,
      config = HttpEngineConfig(),
      buildRequest = ::buildRequest,
      buildResponse = ::buildResponse,
      requestHandlerConfigs = listOf(
          FullRestAdapter<Context, Person, String>(
              assetType = "person",
              dataManager = personDataManager,
              req2Asset = ::requestToPerson,
              buildContext = ::requestToContext,
              asset2Resp = ::personToResponse,
              string2Id = { it }
          )
      )
  )


  engine.start()
}

fun requestToPerson(request: Request): Person = Person("ddf")

fun requestToContext(request: Request): Context = Context("1212")

fun personToResponse(person: Person): Response = Response("Hello World")

fun buildRequest(httpReq: HttpServerRequest): Request {
  return Request(
      action = methodToAction(httpReq.method()),
      path = httpReq.path().split("/".toRegex()).filter { it.isNotEmpty() }
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


data class Person(val name: String)

data class Context(val callerIp: String)

data class HttpEngineConfig(
    val port: Int = 8080,
    val host: String = "localhost"
)


class HttpEngine<REQ, RESP>(
    val name: String,
    vertx: Vertx,
    val config: HttpEngineConfig,
    val buildRequest: (HttpServerRequest) -> REQ,
    val buildResponse: (RESP, HttpServerResponse) -> Unit,

    private val requestHandlerConfigs: List<RequestHandler<REQ, RESP>>
) {
  val server = vertx.createHttpServer()

  init {
    server.requestHandler {
      val req = buildRequest(it)
      var handled = false
      for (rhc in requestHandlerConfigs) {
        if (rhc.isDefinedFor(req)) {
          buildResponse(rhc.invoke(req), it.response())
          handled = true
          break
        }
      }
      if (!handled) {
        it.response().setStatusCode(400).end("UNKNOWN")
      }
    }
  }

  fun start(): Unit {
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

  fun stop() {
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

data class Request(
    val action: Action,
    val path: List<String>
)

data class Response(
    val content: String
)


interface RequestHandler<REQ, RESP> {
  fun isDefinedFor(request: REQ): Boolean
  fun invoke(it: REQ): RESP
}

/**
 *
 */
class FullRestAdapter<C, T, ID>(
    val assetType: String,
    val buildContext: (Request) -> C,
    val req2Asset: (Request) -> T,
    val asset2Resp: (T) -> Response,
    val dataManager: DataManager<T, ID>,
    val string2Id: (String) -> ID
) : RequestHandler<Request, Response> {


  override fun isDefinedFor(request: Request): Boolean =
      request.path[0] == assetType && (request.action == Action.GET || request.action == Action.POST)

  override fun invoke(it: Request): Response =
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


  private fun get(request: Request): Response {
    val id = string2Id(request.path[1])
    val t = dataManager.id(id).onError { throw it }.toBlocking().first()
    return asset2Resp(t)
  }

  private fun insert(request: Request): Response {
    val t = dataManager.insert(req2Asset(request)).onError { throw it }.toBlocking().first()
    return asset2Resp(t)
  }

  private fun todo(request: Request): Response = Response(
      content = "TODO"
  )


}

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

interface DataManager<T, ID> : DataQuery<T, ID> {
  fun insert(t: T): Observable<T>
  fun update(id: ID, t: T): Observable<T>
  fun delete(id: ID): Observable<ID>
}

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





