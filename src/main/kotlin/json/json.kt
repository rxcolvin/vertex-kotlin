package json

import entitymeta.EntityFieldMeta
import entitymeta.EntityMeta
import meta.JSONType

/**
 * Created by richard.colvin on 24/03/2017.
 */


class JSON {

  fun <E : Any> toJSON(t: E, em: EntityMeta<E, *>) {

    println("{")
    println("\t_entityName=${em.entityName}")
    em.entityMetaFields.forEach {
      when (it.fieldMeta.type.jsonType) {
        JSONType.STRING -> toJSONString(it)
        JSONType.NUMBER -> TODO()
        JSONType.LIST -> TODO()
        JSONType.MAP -> TODO()
      }
    }
    println("}")

  }

  fun <E : Any> toJSONString(it: EntityFieldMeta<*, E, *>) {

  }


  fun fromJSON(){}
}
