package entitymeta

import utils.quotize
import java.util.*


/**
 * Created by richard.colvin on 24/03/2017.
 */


class EntityJSONHelper<E : Any, E_ : Any>(
    private val em: EntityMeta<E, E_>,
    private val jsonToMap: (String) -> Map<String, Any?>,
    private val mapToJson: (Map<String, Any?>) -> String
) {

  fun toJson(t: E) = mapToJson(entityToMap(t))
  fun fromJson(json: String) = mapToEntity(jsonToMap(json))

  private fun mapToEntity(
      map: Map<String, Any?>

  ): E {
    val eb = em.builderFactory()
    em.entityMetaFieldMap.forEach {

      val (key, emf) = it
      val v = map[key]
      if (v != null) {
        try {
          val vv = emf.fieldMeta.type.json.fromJsonAny(v)
          val msg2 = emf.fieldMeta.validateAny(vv)
          if (msg2 != null) {
            throw  JSONException("Error in value for ${quotize(emf.fieldMeta.name)} : " + msg2)
          }
          emf.setAny(eb, vv)
        } catch (e: Exception) {
          throw  JSONException("Unexpected type for  ${quotize(emf.fieldMeta.name)} " )
        }
      } else {
        if (!it.value.nullable) {
          throw  JSONException("A value for  ${quotize(emf.fieldMeta.name)} must be provided")

        }
      }
    }

    return em.builder2Entity(eb)
  }

  private fun entityToMap(
      t: E
  ): Map<String, Any?> {

    val mapEntries = ArrayList<Pair<String, Any?>>()

    em.entityMetaFieldMap.forEach {
      val (key, emf) = it
      val vv = emf.get(t)
      val jsonValue = emf.fieldMeta.type.json.toJsonAny(vv)
      mapEntries += key to jsonValue
    }
    return mapEntries.associate { it }
  }
}

class JSONException(msg: String) : Exception(msg)



