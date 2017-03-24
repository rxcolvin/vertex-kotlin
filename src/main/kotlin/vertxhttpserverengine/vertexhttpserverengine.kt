package vertxhttpserverengine

import httpengine.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import utils.Manageable

fun httpServerOptions(
    port: Int
) = HttpServerOptions().setPort(port)

data class HttpEngineConfig(
    val port: Int = 8080,
    val host: String = "localhost"
)

/**
 *  This is a server httpengine that manages http requests using vertxhttpserverengine.
 *
 *  It is general and just needs to know how to convert an vertxhttpserverengine req in to a app REQ
 *  and a app RESP into a vertex resp.
 *
 *  This way the actual httpengine can be replaced by any other technology.
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
    private val processor: (REQ) -> Handler<RESP>?
) : Manageable {
  val server = vertx.createHttpServer(
      httpServerOptions(port = config.port)
  )

  init {
    server.requestHandler {
      val req = buildRequest(it)
      val h = processor.invoke(req)
      val resp = it.response()
      if (h != null) {
        when (h) {

          is Handler.NoBody<RESP> -> {
            it.bodyHandler {
              buildResponse(h.invoke(), resp)
            }
          }
          is Handler.Body<RESP> -> {
            it.bodyHandler {
              buildResponse(h.invoke(String(it.bytes)), resp)
            }
          }
        }
      } else {
        it.bodyHandler {
          resp.setStatusCode(400).end("UNKNOWN")

        }
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
