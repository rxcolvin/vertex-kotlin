package meta2
import kotlin.reflect.KClass


interface  Type<K: Any> {
  val klass: KClass<K>
}


data class AtomicType<K : Any> (
    override val klass : KClass<K>
) : Type<K>

data class AtomicListType<K : Any> (
    override val klass: KClass<List<K>>
) : Type<List<K>>

data class ComplexListType<K:Any> (
    override val klass: KClass<List<K>>,
    val array: Array<Type<K>>
) : Type<List<K>>

data class FieldType<K: Any> (
    val name: String,
    override val klass : KClass<Pair<String, KClass<K>>>,
    val type: Type<K>
)  : Type<Pair<String, KClass<K>>>


data class ComplexType (
    val array: Array<FieldType<*>>
)


fun <K:Any> toJson(item:K, type: Type<K>) {

}