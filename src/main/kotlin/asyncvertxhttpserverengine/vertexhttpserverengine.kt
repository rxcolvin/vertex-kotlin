package asyncvertxhttpserverengine

import httpengine.AsyncHandler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import logging.Logger
import utils.Manageable
import utils.asString
import java.util.concurrent.CompletableFuture

fun httpServerOptions(
    config: HttpEngineConfig
) = HttpServerOptions().setPort(config.port).setHost(config.host)

data class HttpEngineConfig(
    val port: Int = 8080,
    val host: String = "localhost"
)




/**
 *  This is a server httpengine that manages http requests using asyncvertxhttpserverengine.
 *
 *  It is general and just needs to know how to convert an asyncvertxhttpserverengine req in to a app REQ
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
    private val processor: (REQ) -> AsyncHandler<RESP>?,
    private val logger: Logger
) : Manageable {
  val server = vertx.createHttpServer(
      httpServerOptions(config)
  )

  init {
    server.requestHandler {
      logger.debug { ("Action=" + it.method())}
      logger.debug{("Headers ")}
      logger.debug { it.headers().asString() }


      val req = buildRequest(it)
      val h = processor.invoke(req)
      val resp = it.response()
      if (h != null) {
        when (h) {

          is AsyncHandler.NoBody<RESP> -> {
            it.bodyHandler {
              h.invoke().whenComplete {
                s, _ -> buildResponse(s, resp)
              }
            }
          }
          is AsyncHandler.Body<RESP> -> {
            it.bodyHandler {
               h.invoke(it.toString()).whenComplete {
                 s, _ -> buildResponse(s, resp)
              }
            }
          }
        }
      } else {
        //This should never happen:
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
        println("$name is Running on ${config.host}:${config.port}")
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

class VertxCfFactory(
    val vertx: Vertx,
    val ordered: Boolean = false
) {

  /**
   * Convert a function returning whatever into a Compleateable Future
   * of whatever.
   *
   * @param f some blocking process
   */
  fun completableFuture(f: () -> Any): CompletableFuture<Any> {
    val ret = CompletableFuture<Any>()
    vertx.executeBlocking<Any>(
        {
          try {
            val res = f()
            it.complete(res)
          } catch (t: Throwable) {
            it.fail(t.cause)
          }
        },
        ordered,
        {
          if (it.succeeded()) {
            ret.complete(it.result())
          } else {
            ret.completeExceptionally(it.cause())
          }

        }
    )
    return ret
  }

}
