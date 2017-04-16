/**
 * Created by richard on 01/01/2017.
 */
package testing

import kotlin.reflect.KClass
import kotlin.reflect.KFunction


open class TestItem(
        val name: String,
        val tags: List<String>,
        val enabled:Boolean = true

)

class Test(
        name: String,
        tags: List<String> = emptyList(),
        val runner: () -> Unit
) : TestItem(name, tags)


class TestGroup(
        name: String,
        tags: List<String> = emptyList(),
        val tests: List<Test> = emptyList(),
        val groups: List<TestGroup> = emptyList()
) : TestItem(name, tags)



interface TestReport {
    fun startGroup(testGroup: TestGroup)
    fun startTest(test: Test): Long
    fun passTest(id: Long)
    fun failTest(id: Long, e: Exception)
}


class TestFailed(msg:String) : Exception(msg)

class ConsoleTestReport : TestReport {
    override fun startGroup(testGroup: TestGroup) {
        println("Starting Tests for Group ${testGroup.name}")
    }

    override fun startTest(test: Test): Long {
        print("Starting Test  ${test.name} ..")

        return System.currentTimeMillis()
    }

    override fun passTest(id: Long) {
        println("passed: took ${System.currentTimeMillis() - id}")
    }

    override fun failTest(id: Long, e: Exception) {
        println("passed: took ${System.currentTimeMillis() - id}")
        if (e is TestFailed)
            println(e.message)
        else
            e.printStackTrace()
    }

}

fun run(
        testGroup: TestGroup,
        report: TestReport,
        filter: (TestItem) -> Boolean = {true}
        ) {
    report.startGroup(testGroup)
    testGroup.tests.filter {it.enabled}.filter(filter).forEach { run(it, report ) }
    testGroup.groups.filter {it.enabled}.filter(filter).forEach { run(it, report ) }
}

fun run(
        test: Test,
        report: TestReport
) {
    val id = report.startTest(test)
    try {
        test.runner()
        report.passTest(id)
    } catch (e: Exception) {
        report.failTest(id, e)
    }

}

open class ClassTest<T: Any> (
        val kClass: KClass<T>
)


open class MethodTest<T:Any> (
        val kClass: KClass<T>,
        val kFunction: KFunction<Unit>
)



fun <T> assertThat( target:T,  matches: (T) -> Pair<Boolean, String>) {
    val (flag, msg) = matches(target)
    if (!flag) throw TestFailed(msg)
}



/*

    val tests = tests("all") {
        testingClass (EntityMeta::Class) {
            testingMethod(??) {
                Given ("Desc") {
                }
                Then ("Desc") {
                    it.something
                }Should {
                    Match ("the number 10") {
                        ::equals(10)
                    }
                    Match ("Desc" {
                    }
                }
                Then
        }
        testingFunction(xx) {
          test ( {
            "foo"
            } matches (String::equals)
          test "foo" using String::equals

 */
