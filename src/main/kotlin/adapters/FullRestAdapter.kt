package adapters

import datamanager.DataManager
import http.Action
import http.Request
import http.Response
import httpengine.Handler
import rx.lang.kotlin.onError

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
) : Function1<Request, Handler<Response>?> {

  inner class Get(
      private val request: Request
  ) : Handler.NoBody<Response> {
    override fun invoke(): Response {
      val id = string2Id(request.path[1])
      val t = dataManager.id(id).onError { throw it }.toBlocking().first()
      return Response(
          content = asset2String(t)
      )
    }
  }

  inner class Post(
      private val request: Request
  ) : Handler.Body<Response> {


    override fun invoke(text: String): Response {
      val t = dataManager.insert(string2Asset(text)).onError { throw it }.toBlocking().first()
      return Response(
          content = asset2String(t)
      )
    }
  }


  override fun invoke(
      req: Request
  ): Handler<Response>? =
      if (req.path[0] == assetType) {
        when (req.action) {
          Action.GET -> Get(req)
          Action.POST -> Post(req)
          else -> null
        }
      } else null

}