package adapters

import httpengine.AsyncHandler
import logging.Logger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

class AsyncLoggingAdapter<RQ, RP>(
    val path: String,
    val pathResolver: (RQ) -> String,
    val actionResolver: (RQ) -> String,
    val respFactory: () -> RP,
    val logger: Logger

) : (RQ) -> AsyncHandler<RP>? {
  override fun invoke(req: RQ): AsyncHandler<RP>? =
      if (pathResolver(req) == path) Handler(req) else null


  inner class Handler(
      private val request: RQ
  ) : AsyncHandler.Body<RP> {
    override fun invoke(text: String): CompletableFuture<RP> =
        CompletableFuture.supplyAsync({
          logger.info {LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)}
          logger.info{actionResolver(request)}
          logger.info{pathResolver(request)}
          logger.info{text.length }
          text.forEach { if (it.toInt() == 13) println() else print(it) }
          val resp = respFactory()
          println("====================")
          println(resp)
          println("====================")
          resp
        })
  }
}