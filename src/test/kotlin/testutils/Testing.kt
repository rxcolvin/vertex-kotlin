package testutils

import utils.into
import utils.timeIt
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

typealias Instance<T> = () -> T
typealias Calling<T, R> = (T) -> R
typealias Expect<R> = (R) -> String?

//Basics


interface Test {
  val description: String
  fun run(): TestResult
}

interface TestGroup {
  val description: String
  val tests: List<Test>
  val testGroups: List<TestGroup>
}

data class TestResult(
    val test: Test,
    val passed: Boolean,
    val message: String?,
    val duration: Long,
    val exception: Throwable?,
    val time: LocalDateTime = LocalDateTime.now()
)


/**
 * Single test for a given class and method
 */
data class MethodTest<T : Any, R>(
    val klass: KClass<T>,
    val method: KFunction<R>,
    override val description: String,
    val instance: Instance<T>,
    val calling: Calling<T, R>,
    val expect: Expect<R>
) : Test {
  override fun run(): TestResult {
    val (duration, msg, t) = timeIt { expect(calling(instance())) }
    return TestResult(this, msg == null, msg, duration, t)
  }
}

data class MethodTests<T : Any, R>(
    val klass: KClass<T>,
    val method: KFunction<R>,
    override val tests: List<MethodTest<T, R>>
) : TestGroup {
  override val testGroups: List<TestGroup> = emptyList()
  override val description = "Tests for method ${method.name}"
}


data class ClassTests<T : Any>(
    val klass: KClass<T>,
    override val testGroups: List<MethodTests<T, *>>
) : TestGroup {
  override val description: String = "Tests for class ${klass.qualifiedName}"
  override val tests: List<MethodTest<T, out Any>> = emptyList()

}


// Expectations

class Equals<R>(
    val v: R
) : Expect<R> {
  override fun invoke(p1: R): String? = if (v != p1) "Expected [$v] got [$p1]" else null
}

// Runners

interface ResultCollector {
  fun insert(result: TestResult)
}

interface GroupResultCollector {

}

interface Runner {
  fun run(test: Test)
  fun run(testGroup: TestGroup)
}

class PrintRunner : Runner {
  override fun run(test: Test) {
    test.run().print()
  }

  override fun run(testGroup: TestGroup) {
    println("Running tests for ${testGroup.description}")
    testGroup.tests.forEach {
      run(it)
    }
    testGroup.testGroups.forEach { run(it) }
  }
}

// Print

fun TestResult.print() {
  print("${this.passed.passOrFail()}\t [${this.test.description}]")
  if (!this.passed) {
    print("\t${this.message}")
  }
  println(" in ${this.duration} ms")
}


fun Boolean.passOrFail() = if (this) "PASSED" else "FAILED"


//DSL

fun <T : Any> classTests(klass: KClass<T>, init: ClassTests_<T>.() -> Unit): ClassTests_<T> {
  val ret = ClassTests_<T>(klass)
  ret.init()
  return ret
}


class ClassTests_<T : Any>(
    val klass: KClass<T>
) {

  val tests = mutableListOf<MethodTests_<T, *>>()

  fun <R> methodTests(
      m: KFunction<R>,
      init: MethodTests_<T, R>.() -> Unit
  ): MethodTests_<T, R> {
    val ret = MethodTests_<T, R>(kclass = klass, method = m)
    ret.init()
    tests.add(ret)
    return ret
  }
}

class MethodTests_<T : Any, R>(
    val kclass: KClass<T>,
    val method: KFunction<R>
) {
  val tests = mutableListOf<MethodTest_<T, R>>()

  fun methodTest(
      desc:String,
      init: MethodTest_<T, R>.() -> Unit
      ) : MethodTest_<T, R> {

    val ret  = MethodTest_<T, R>(kclass, method, desc)
    ret.init()
    tests.add(ret)

    return ret
  }
}

class MethodTest_<T : Any, R>(
    val kclass: KClass<T>,
    val m: KFunction<R>,
    val description: String
) {

  var instance: Instance<T>? = null
  var calling: Calling<T, R>? = null
  var expect: Expect<R>? = null

    fun given(f: Instance<T>) {
    instance = f
  }

  fun calling(f: Calling<T, R>) {
    calling = f
  }

  fun expect(f: () -> Expect<R>) {
    expect = f()
  }
}


fun ff() = classTests(TestMe::class) {
  methodTests(TestMe::hello) {
    methodTest("When blah") {
      given { TestMe() }
      calling { it.hello() }
      expect { Equals("Hello") }
    }
  }

}


fun <T: Any> ClassTests_<T>.create() = ClassTests(
    klass = this.klass,
    testGroups = this.tests.map { it.create() }
)


fun <T: Any, R> MethodTests_<T, R>.create() : MethodTests<T, R> {

  val ret = MethodTests<T, R>(
      klass = this.kclass,
      method = this.method,
      tests = this.tests.map { it.create() }
  )

  return ret
}

fun <T: Any, R> MethodTest_<T, R>.create() : MethodTest<T, R> {

  val ret = MethodTest<T, R>(
      klass = this.kclass,
      method = this.m,
      description = this.description,
      instance = this.instance!!,
      calling = this.calling!!,
      expect = this.expect!!
  )

  return ret;
}


class TestMe {
  fun hello() = "Hello"
}

fun main(args: Array<String>) {
  PrintRunner().run(
      ClassTests(
          klass = TestMe::class,
          testGroups = listOf(
              MethodTests(
                  klass = TestMe::class,
                  method = TestMe::hello,
                  tests = listOf(
                      MethodTest(
                          method = TestMe::hello,
                          klass = TestMe::class,
                          description = "Given a testutils.TestMe then when I call hello I expect the value 'Hello' to be returned",
                          instance = { TestMe() },
                          calling = { it.hello() },
                          expect = Equals("Hell")
                      )
                  )
              )
          )
      )
  )

  PrintRunner().run(ff().create())
}

