/**
 * Created by richard on 30/12/2016.
 */

package tuples

data class T2<out P1, out P2>(
        val p1: P1,
        val p2: P2

) {
    override fun toString(): String = "($p1, $p2)"
}

data class T3<out P1, out P2, out P3>(
        val p1: P1,
        val p2: P2,
        val p3: P3

) {
    override fun toString(): String = "($p1, $p2, $p3)"
}

