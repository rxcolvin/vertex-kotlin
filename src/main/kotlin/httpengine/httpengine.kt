package httpengine

import java.util.concurrent.CompletableFuture


interface Handler<RESP> {

  interface NoBody<RESP> : Handler <RESP> {
    fun invoke(): RESP
  }

  interface Body<RESP> : Handler <RESP> {
    fun invoke(text: String): RESP
  }

  interface StreamBody<RESP> : Handler <RESP> {
    fun invoke(stream: Iterator<String>): RESP
  }

  //+ Others?? (Form?)
}


interface AsyncHandler<RESP> {

  interface NoBody<RESP> : AsyncHandler <RESP> {
    fun invoke(): CompletableFuture<RESP>
  }

  interface Body<RESP> : AsyncHandler <RESP> {
    fun invoke(text: String): CompletableFuture<RESP>
  }

  interface StreamBody<RESP> : AsyncHandler <RESP> {
    fun invoke(stream: Iterator<String>): CompletableFuture<RESP>
  }

  //+ Others?? (Form?)
}









