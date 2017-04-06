package simplerserverapp

import adapters.FullRestAdapter
import http.Action
import http.Request
import http.Response
import inmemorydatamanager.InMemoryDataManager
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.kotlin.core.VertxOptions
import simplerserverapp.personMeta.personJsonHelper
import vertxhttpserverengine.HttpEngineConfig
import vertxhttpserverengine.VertxHttpServerEngine

/**
 * Created by richard.colvin on 24/03/2017.
 */
fun main(args: Array<String>) {

  val vertx = Vertx.vertx(VertxOptions().setBlockedThreadCheckInterval(999999L))

  val personDataManager = InMemoryDataManager<Person, String>(
      Person::uuid
  )

  val inserted = personDataManager.insert(
      Person(
          uuid = "1",
          firstName = "John",
          surname = "Doe",
          age = 34
      )
  ).toBlocking().first()


  val engine = VertxHttpServerEngine<Request, Response>(
      name = "REST Server",
      vertx = vertx,
      config = HttpEngineConfig(),
      buildRequest = ::buildRequest,
      buildResponse = ::buildResponse,
      processor =
      FullRestAdapter<Context, Person, String>(
          assetType = "person",
          dataManager = personDataManager,
          string2Asset = personJsonHelper::fromJson,
          buildContext = ::requestToContext,
          asset2String = personJsonHelper::toJson,
          string2Id = { it },
          exception2Resp = ::exceptionToResponse
      )
  )



  engine.start()
}








fun requestToContext(request: Request): Context = Context("1212")


fun exceptionToResponse(exception: Exception) =
    Response(
        content = exception.toString()
    )


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




data class Context(val callerIp: String)
