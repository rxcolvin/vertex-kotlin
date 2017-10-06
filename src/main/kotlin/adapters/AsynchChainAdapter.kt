package adapters

import http.Request
import http.Response
import httpengine.AsyncHandler
import httpengine.Handler

/**
 * Created by richard.colvin on 02/06/2017.
 */

class AsyncChainAdapterChainAdapter<Req, Resp> (
    val adapters: Array<(Req) -> AsyncHandler<Resp>?>
): (Req) -> AsyncHandler<Resp>? {
  override fun invoke(p1: Req): AsyncHandler<Resp>? {
    for(adapter in adapters) {
      val resp = adapter.invoke(p1)
      if (resp != null) {
        return resp
      }
    }
    return null
  }
}