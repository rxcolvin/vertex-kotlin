package http

enum class Action {
  GET, PUT, POST, DELETE, OPTIONS, PATCH, HEAD, TRACE, CONNECT, OTHER
}

/**
 * Embryonic Representation of a general request
 */
data class Request(
    val action: Action,
    val path: List<String>
)

/**
 * Embryonic Representation of an Application Response
 */
data class Response(
    val status: Int = 200,
    val content: String = "OK"
)