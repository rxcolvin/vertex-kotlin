package adapters

import asynchdatamanager.AsyncDataManager
import datamanager.QueryDef
import httpengine.AsyncHandler
import logging.Logger
import java.util.concurrent.CompletableFuture

/**
 *
 */
class AsyncFullRestAdapter<RQ, RP, C, T, ID>(
    val assetType: String,
    val buildContext: (RQ) -> C,
    val string2Asset: (String) -> T,
    val asset2String: (T) -> String,
    val assets2String: (Sequence<T>) -> String,
    val dataManager: AsyncDataManager<T, ID>,
    val idResolver: (RQ) -> ID,
    val queryDefResolver: (RQ) -> QueryDef,
    val assetNameResolver: (RQ) -> String,
    val respFactory: (String) -> RP,
    val getResolver: (RQ) -> Boolean,
    val queryResolver: (RQ) -> Boolean,
    val postResolver: (RQ) -> Boolean,
    val assetIdResolver: (T) -> ID,
    val logger: Logger = Logger(name = "FullRestAdapterDefault", debugEnabled = true)
) : (RQ) -> AsyncHandler<RP>? {

  inner class GetById(
      private val request: RQ
  ) : AsyncHandler.NoBody<RP> {
    override fun invoke(): CompletableFuture<RP> {
      val id = idResolver(request)
      logger.debug { "GetById assetName=${assetNameResolver(request)} id=$id" }
      return dataManager.id(id).thenApply {
        respFactory(asset2String(it))
      }
    }
  }

  inner class Query(
      private val request: RQ
  ) : AsyncHandler.NoBody<RP> {
    override fun invoke(): CompletableFuture<RP> {
      logger.debug { "Query assetName=${assetNameResolver(request)}" }

      return dataManager.query(
          queryDefResolver(request)).thenApply {
        respFactory(assets2String(it))
      }
    }
  }

  inner class Post(
      private val request: RQ
  ) : AsyncHandler.Body<RP> {


    override fun invoke(text: String): CompletableFuture<RP> {
      //TODO: should validate the request id and the asset id are the same
      val id = idResolver(request)
      val asset = string2Asset(text)
      val assetId = assetIdResolver(asset)
      if (id != assetId) {
        throw InconsistentIdException(
            assetId = assetId.toString(),
            pathId = id.toString()
        )
      }

      logger.debug { "Post assetName=${assetNameResolver(request)} assetId=$assetId}" }
      return dataManager.insert(asset).thenApply {
        respFactory(asset2String(it))
      }
    }
  }

  override fun invoke(
      req: RQ
  ): AsyncHandler<RP>? =
      if (assetNameResolver(req) == assetType) {
        if (getResolver(req)) {
          GetById(req)
        } else if (queryResolver(req)) {
          Query(req)
        } else if (postResolver(req)){
          Post(req)
        } else
          null
      } else null
}

class InconsistentIdException(assetId: String, pathId: String) : Exception()


