package json

import io.vertx.core.json.JsonObject

/**
 * Created by richard.colvin on 06/04/2017.
 */

val jsonToMap:  (String) -> Map<String, Any?> = { JsonObject(it).map }
val mapToJson: (Map<String, Any?>) -> String = { JsonObject(it).encodePrettily() }
