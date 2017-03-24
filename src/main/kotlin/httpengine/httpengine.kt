package httpengine


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











