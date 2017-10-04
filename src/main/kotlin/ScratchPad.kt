package scratchpad


import utils.quotize

interface Expr

interface BoolExpr : Expr

interface Value<T> : Expr

data class And(val exprs: List<BoolExpr>) : BoolExpr

data class Not(val expr: BoolExpr) : BoolExpr

data class Or(val expr: List<BoolExpr>) : BoolExpr

data class Between<T : Number>(val value: Value<T>, val min: Value<T>, val max: Value<T>) : BoolExpr

data class In<T>(val value: Value<T>, val values: List<Value<T>>)

data class Const<T>(val value: T) : Value<T>

data class Field<T>(val id: String) : Value<T>

data class Eq<T>(val left: Value<T>, val right: Value<T>) : BoolExpr

data class Like(val value: Value<String>, val like: String) : BoolExpr


fun eval(
    test: BoolExpr,
    f: (String) -> Any
): Boolean {
  return true
}

fun toEnglish(test: Expr): String =
    when (test) {
      is Const<*> -> quotize(test.value)
      is Field<*> -> quotize(test.id)
      is And -> test.exprs.map { toEnglish(it) }.joinToString(prefix = "(", postfix = ")", separator = " AND ")
      is Eq<*> -> toEnglish(test.left) + " == " + toEnglish(test.right)
      else -> "TODO"
    }

fun quotize(v: Any?): String =
    when (v) {
      null -> "@NULL@"
      is String -> '"' + v + '"'
      else -> v.toString()
    }

fun encode(test: Any): String {
  fun encodeConst(c: Const<*>) = scratchpad.quotize(c.value)
  fun encodeAnd(and: And): String = "And(" +
      and.exprs.map { encode(it) }.joinToString(separator = ", ") + ")"

  fun encodeField(field: Field<*>): String = "Field(${scratchpad.quotize(field.id)})"
  fun encodeEq(eq: Eq<*>): String = "Eq(" + encode(eq.left) + ", " + encode(eq.right) + ")"

  return when (test) {
    is Const<*> -> encodeConst(test)
    is Field<*> -> encodeField(test)
    is And -> encodeAnd(test)
    is Eq<*> -> encodeEq(test)
    else -> "TODO"
  }

}

//fun decode(text: String): Expr {
//
//  fun decodeConst(params:  List<String>): Const<*> {
//    return Const(value = 10)
//  }
//
//  fun decodeField(params:  List<String>): Field<*> {
//    return Field<Any>(id=params[0])
//  }
//
//  fun decodeEq(params:  List<String>): Eq<*> {
//    return Eq<Any>(left = decode(params[0]) as Value<Any>, right = decode(params[1]) as Value<Any>)
//  }
//
//  fun decodeAnd(params: List<String>): Expr {
//    //TODO: needs exceptions
//    return And(exprs = params.map { decode(it) }.filter { it is BoolExpr }.toList() as List<BoolExpr>)
//  }


fun parse(t: String): Any = parse(ParserHelper(t))

fun parse(k: ParserHelper): Any {
  k.skipWS()
  val c = k.peek()
  return when {
    c == '"' -> parseString(k)
    c.isLetter() -> parseExpr(k)
    c.isDigit() || c in arrayOf('-', '+', '.') -> parseNumber(k)
    c == '#' -> parseId(k)
    else -> throw RuntimeException("")
  }
}

enum class State { TYPE, PARAMS, OPEN }

fun parseId(k: ParserHelper): String {
  k.skipWS()
  val b = StringBuilder()
  var done = false
  var isId = false
  while (k.hasNext() && !done) {
    if (!isId) {
      val c = k.next()
      if (c == '#') {
        isId = true
      } else {
        throw RuntimeException("Expected a #")
      }
    } else {
      val c = k.peek()
      if (c.isLetter()) {
        b.append(c)
        k.consume()
      } else {
        done = true
      }
    }
  }
  val ret = b.toString()
  if (ret.isEmpty()) {
    throw RuntimeException("Expected an id")
  }
  return ret

}


fun parseExpr(k: ParserHelper): Expr {
  k.skipWS()

  var state = State.TYPE
  val key = StringBuffer()
  val params = mutableListOf<Any>()
  var done = false

  while (!done && k.hasNext()) {
    when (state) {
      State.TYPE -> {
        val c = k.peek()

        if (c.isLetter()) {
          key.append(c)
          k.consume()
        } else {
          k.skipWS()
          state = State.OPEN
        }
      }
      State.OPEN -> {
        val c = k.next()
        if (c == '(') {
          state = State.PARAMS
        } else if (!c.isWhitespace()) {
          throw RuntimeException("Parse Error")
        }
      }
      State.PARAMS -> {
        k.skipWS()
        val c = k.peek()
        if (c == ',') {
          k.consume()
        } else if (c == ')') {
          k.consume()
          done = true
        } else {
          params.add(parse(k))
        }
      }

    }
  }
  if (!done) {
    throw RuntimeException("Unexpected end of text")
  }

  return when (key.toString()) {
    "And" -> And(params.toList() as List<BoolExpr>) //TODO Nicer errors
    "Const" -> Const<Any>(params[0])
    "Field" -> Field<Any>(params[0] as String)
    "Eq" -> Eq<Any>(makeConstMaybe(params[0]), makeConstMaybe(params[1]))
    else -> throw RuntimeException("Unknown Type $key")
  }
}

fun makeConstMaybe(v: Any): Value<Any> = when (v) {
  is String, is Number -> Const(v)
  else -> v as Value<Any>
}


fun parseString(k: ParserHelper): String {
  var started = false
  val b = StringBuilder()
  k.skipWS()
  var done = false
  while (k.hasNext() && !done) {
    val c = k.next()
    if (!started) {
      if (c != '"') {
        throw RuntimeException("")
      }
      started = true
    } else {
      if (c == '"') {
        done = true
      } else {
        b.append(c)
      }
    }
  }

  return b.toString()
}

fun parseNumber(k: ParserHelper): Number {
  k.skipWS()
  var sign = 1
  val b = StringBuilder()
  var done = false
  var isDecimal = false
  while (k.hasNext() && !done) {
    val c = k.peek()
    when {
      c == '-' -> {
        sign = -1;k.consume()
      }
      c == '+' -> {
        sign = +1;k.consume()
      }
      c.isDigit() -> {
        b.append(c);k.consume()
      }
      c == '.' -> {
        if (isDecimal) throw RuntimeException("Dotty") else isDecimal = true; b.append(c)
      }
      else -> done = true
    }
  }

  val v = b.toString()
  if (v.isEmpty()) {
    throw RuntimeException("Expected a number")
  }
  if (v == ".") {
    throw RuntimeException("Bad Number " + v)
  }

  return if (isDecimal) v.toDouble() * sign else v.toLong() * sign
}





fun main(args: Array<String>) {

  val testText = """
    And(
      Eq(Field("foo"), 26),
      Field(#someBoolean)
      )
    """
  println(parse(testText))

  val t = """
    Eq(Const(10), Field("foo")), Eq(Const(25), Field("bar"))
    """
  println(parseExpr(ParserHelper("And(Eq(\"Foo\", Field(\"bar\")))")))
  println(parseExpr(ParserHelper("Const(10)")))

  println(parseString(ParserHelper("\"Hello\"")))
  val test = And(
      listOf(
          Eq(Const(10), Field("foo")), Eq(Const(25), Field("bar"))
      )
  )

  val text = encode(parse("And(Eq(\"Foo\", Field(\"bar\")))"))
  println(text)


}



/**
 * Naf-ish implementation
 *
 * Not Threadsafe
 */
class ParserHelper(stream: CharSequence) {
  val k: Iterator<Char> = stream.iterator()

  var peek: Char? = if (k.hasNext()) k.next() else null

  fun peek(): Char {
    return peek!!
  }

  fun hasNext(): Boolean =
      peek != null


  fun next(): Char {
    val ret = peek!!
    peek = if (k.hasNext()) k.next() else null
    return ret
  }

  fun consume() {
    next()
  }

  fun skipWS() {
    while (this.hasNext() && this.peek().isWhitespace()) this.consume()
  }
}




