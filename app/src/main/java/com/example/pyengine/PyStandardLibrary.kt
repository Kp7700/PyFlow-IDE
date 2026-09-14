package com.example.pyengine

import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

object PyStandardLibrary {

    fun createBuiltinModules(fileIO: PyFileIO): Map<String, PyModule> {
        val modules = mutableMapOf<String, PyModule>()

        // math
        val math = PyModule("math")
        math.members["pi"] = PyFloat(Math.PI)
        math.members["e"] = PyFloat(Math.E)
        math.members["tau"] = PyFloat(2 * Math.PI)
        math.members["inf"] = PyFloat(Double.POSITIVE_INFINITY)
        math.members["nan"] = PyFloat(Double.NaN)

        math.members["sqrt"] = PyBuiltinFunction("sqrt") { args, _, _ ->
            val v = getDouble(args, 0, "sqrt")
            if (v < 0) throw PyRuntimeError("ValueError", "math domain error")
            PyFloat(sqrt(v))
        }
        math.members["sin"] = PyBuiltinFunction("sin") { args, _, _ -> PyFloat(sin(getDouble(args, 0, "sin"))) }
        math.members["cos"] = PyBuiltinFunction("cos") { args, _, _ -> PyFloat(cos(getDouble(args, 0, "cos"))) }
        math.members["tan"] = PyBuiltinFunction("tan") { args, _, _ -> PyFloat(tan(getDouble(args, 0, "tan"))) }
        math.members["asin"] = PyBuiltinFunction("asin") { args, _, _ -> PyFloat(asin(getDouble(args, 0, "asin"))) }
        math.members["acos"] = PyBuiltinFunction("acos") { args, _, _ -> PyFloat(acos(getDouble(args, 0, "acos"))) }
        math.members["atan"] = PyBuiltinFunction("atan") { args, _, _ -> PyFloat(atan(getDouble(args, 0, "atan"))) }
        math.members["atan2"] = PyBuiltinFunction("atan2") { args, _, _ ->
            val y = getDouble(args, 0, "atan2")
            val x = getDouble(args, 1, "atan2")
            PyFloat(atan2(y, x))
        }
        math.members["floor"] = PyBuiltinFunction("floor") { args, _, _ -> PyInt(floor(getDouble(args, 0, "floor")).toLong()) }
        math.members["ceil"] = PyBuiltinFunction("ceil") { args, _, _ -> PyInt(ceil(getDouble(args, 0, "ceil")).toLong()) }
        math.members["trunc"] = PyBuiltinFunction("trunc") { args, _, _ -> PyInt(truncate(getDouble(args, 0, "trunc")).toLong()) }
        math.members["fabs"] = PyBuiltinFunction("fabs") { args, _, _ -> PyFloat(abs(getDouble(args, 0, "fabs"))) }
        math.members["pow"] = PyBuiltinFunction("pow") { args, _, _ ->
            val x = getDouble(args, 0, "pow")
            val y = getDouble(args, 1, "pow")
            PyFloat(x.pow(y))
        }
        math.members["exp"] = PyBuiltinFunction("exp") { args, _, _ -> PyFloat(exp(getDouble(args, 0, "exp"))) }
        math.members["log"] = PyBuiltinFunction("log") { args, _, _ ->
            val x = getDouble(args, 0, "log")
            if (x <= 0) throw PyRuntimeError("ValueError", "math domain error")
            if (args.size > 1) {
                val base = getDouble(args, 1, "log")
                if (base <= 0 || base == 1.0) throw PyRuntimeError("ValueError", "math domain error")
                PyFloat(ln(x) / ln(base))
            } else {
                PyFloat(ln(x))
            }
        }
        math.members["log10"] = PyBuiltinFunction("log10") { args, _, _ ->
            val x = getDouble(args, 0, "log10")
            if (x <= 0) throw PyRuntimeError("ValueError", "math domain error")
            PyFloat(log10(x))
        }
        math.members["log2"] = PyBuiltinFunction("log2") { args, _, _ ->
            val x = getDouble(args, 0, "log2")
            if (x <= 0) throw PyRuntimeError("ValueError", "math domain error")
            PyFloat(log2(x))
        }
        math.members["radians"] = PyBuiltinFunction("radians") { args, _, _ -> PyFloat(Math.toRadians(getDouble(args, 0, "radians"))) }
        math.members["degrees"] = PyBuiltinFunction("degrees") { args, _, _ -> PyFloat(Math.toDegrees(getDouble(args, 0, "degrees"))) }
        math.members["hypot"] = PyBuiltinFunction("hypot") { args, _, _ ->
            val x = getDouble(args, 0, "hypot")
            val y = getDouble(args, 1, "hypot")
            PyFloat(hypot(x, y))
        }
        math.members["factorial"] = PyBuiltinFunction("factorial") { args, _, _ ->
            val n = getLong(args, 0, "factorial")
            if (n < 0) throw PyRuntimeError("ValueError", "factorial() not defined for negative values")
            var res = 1L
            for (i in 2..n) res *= i
            PyInt(res)
        }
        math.members["gcd"] = PyBuiltinFunction("gcd") { args, _, _ ->
            var a = abs(getLong(args, 0, "gcd"))
            var b = abs(getLong(args, 1, "gcd"))
            while (b != 0L) {
                val temp = b
                b = a % b
                a = temp
            }
            PyInt(a)
        }
        math.members["isqrt"] = PyBuiltinFunction("isqrt") { args, _, _ ->
            val n = getLong(args, 0, "isqrt")
            if (n < 0) throw PyRuntimeError("ValueError", "isqrt() only defined for nonnegative integers")
            PyInt(sqrt(n.toDouble()).toLong())
        }
        modules["math"] = math

        // random
        var rng: kotlin.random.Random = kotlin.random.Random.Default
        val random = PyModule("random")
        random.members["random"] = PyBuiltinFunction("random") { _, _, _ -> PyFloat(rng.nextDouble()) }
        random.members["uniform"] = PyBuiltinFunction("uniform") { args, _, _ ->
            val a = getDouble(args, 0, "uniform")
            val b = getDouble(args, 1, "uniform")
            PyFloat(a + (b - a) * rng.nextDouble())
        }
        random.members["randint"] = PyBuiltinFunction("randint") { args, _, _ ->
            val a = getLong(args, 0, "randint")
            val b = getLong(args, 1, "randint")
            if (a > b) throw PyRuntimeError("ValueError", "empty range for randint($a, $b)")
            val v = a + (rng.nextLong(b - a + 1))
            PyInt(v)
        }
        random.members["randrange"] = PyBuiltinFunction("randrange") { args, _, _ ->
            val start = if (args.size > 1) getLong(args, 0, "randrange") else 0L
            val stop = if (args.size > 1) getLong(args, 1, "randrange") else getLong(args, 0, "randrange")
            val step = if (args.size > 2) getLong(args, 2, "randrange") else 1L
            val range = PyRange(start, stop, step).toList()
            if (range.isEmpty()) throw PyRuntimeError("ValueError", "empty range for randrange()")
            range[rng.nextInt(range.size)]
        }
        random.members["choice"] = PyBuiltinFunction("choice") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "choice() missing 1 required positional argument")
            when (seq) {
                is PyList -> if (seq.elements.isEmpty()) throw PyRuntimeError("IndexError", "Cannot choose from an empty sequence") else seq.elements[rng.nextInt(seq.elements.size)]
                is PyTuple -> if (seq.elements.isEmpty()) throw PyRuntimeError("IndexError", "Cannot choose from an empty sequence") else seq.elements[rng.nextInt(seq.elements.size)]
                is PyStr -> if (seq.value.isEmpty()) throw PyRuntimeError("IndexError", "Cannot choose from an empty sequence") else PyStr(seq.value[rng.nextInt(seq.value.length)].toString())
                else -> throw PyRuntimeError("TypeError", "object of type '${seq.typeName()}' has no len()")
            }
        }
        random.members["shuffle"] = PyBuiltinFunction("shuffle") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "shuffle() missing 1 required positional argument")
            if (seq !is PyList) throw PyRuntimeError("TypeError", "shuffle() requires a mutable sequence")
            seq.elements.shuffle()
            PyNone
        }
        random.members["sample"] = PyBuiltinFunction("sample") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "sample() missing required argument")
            val k = getLong(args, 1, "sample").toInt()
            val list = when (seq) {
                is PyList -> seq.elements
                is PyTuple -> seq.elements
                is PyStr -> seq.value.map { PyStr(it.toString()) }
                is PySet -> seq.set.toList()
                else -> throw PyRuntimeError("TypeError", "Population must be a sequence or set")
            }
            if (k < 0 || k > list.size) throw PyRuntimeError("ValueError", "Sample larger than population or is negative")
            val sampled = list.shuffled().take(k).toMutableList()
            PyList(sampled)
        }
        random.members["seed"] = PyBuiltinFunction("seed") { args, _, _ ->
            val s = if (args.isNotEmpty()) getLong(args, 0, "seed") else System.currentTimeMillis()
            rng = Random(s)
            PyNone
        }
        modules["random"] = random

        // time
        val time = PyModule("time")
        time.members["time"] = PyBuiltinFunction("time") { _, _, _ -> PyFloat(System.currentTimeMillis() / 1000.0) }
        time.members["monotonic"] = PyBuiltinFunction("monotonic") { _, _, _ -> PyFloat(System.nanoTime() / 1_000_000_000.0) }
        time.members["perf_counter"] = PyBuiltinFunction("perf_counter") { _, _, _ -> PyFloat(System.nanoTime() / 1_000_000_000.0) }
        time.members["ctime"] = PyBuiltinFunction("ctime") { _, _, _ ->
            val sdf = SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US)
            PyStr(sdf.format(Date()))
        }
        time.members["sleep"] = PyBuiltinFunction("sleep") { args, _, ctx ->
            val secs = getDouble(args, 0, "sleep")
            if (secs > 0) {
                val ms = (secs * 1000).toLong()
                // Sleep with active check chunks to support quick cancellation
                var remaining = ms
                while (remaining > 0) {
                    ctx.checkActive()
                    val chunk = min(remaining, 100L)
                    delay(chunk)
                    remaining -= chunk
                }
            }
            PyNone
        }
        modules["time"] = time

        // datetime
        val datetime = PyModule("datetime")
        datetime.members["now"] = PyBuiltinFunction("now") { _, _, _ ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US)
            PyStr(sdf.format(Date()))
        }
        datetime.members["today"] = PyBuiltinFunction("today") { _, _, _ ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            PyStr(sdf.format(Date()))
        }
        modules["datetime"] = datetime

        // sys
        val sys = PyModule("sys")
        sys.members["version"] = PyStr("3.12.0 (PyPocket Kotlin/Android JVM Engine)")
        sys.members["platform"] = PyStr("android")
        sys.members["maxsize"] = PyInt(Long.MAX_VALUE)
        sys.members["argv"] = PyList(mutableListOf(PyStr("main.py")))
        sys.members["exit"] = PyBuiltinFunction("exit") { args, _, _ ->
            val code = if (args.isNotEmpty()) getLong(args, 0, "exit") else 0L
            throw PyRuntimeError("SystemExit", code.toString())
        }
        modules["sys"] = sys

        // json
        val json = PyModule("json")
        json.members["dumps"] = PyBuiltinFunction("dumps") { args, _, _ ->
            val obj = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "dumps() missing 1 required argument")
            PyStr(pyToJson(obj))
        }
        json.members["loads"] = PyBuiltinFunction("loads") { args, _, _ ->
            val strVal = getStr(args, 0, "loads")
            jsonToPy(strVal.trim())
        }
        modules["json"] = json

        // os & os.path
        val os = PyModule("os")
        val osPath = PyModule("os.path")
        osPath.members["join"] = PyBuiltinFunction("join") { args, _, _ ->
            val parts = args.map { (it as? PyStr)?.value ?: it.str() }
            PyStr(parts.joinToString("/"))
        }
        osPath.members["exists"] = PyBuiltinFunction("exists") { args, _, _ ->
            val path = getStr(args, 0, "exists")
            PyBool(fileIO.exists(path))
        }
        osPath.members["basename"] = PyBuiltinFunction("basename") { args, _, _ ->
            val path = getStr(args, 0, "basename")
            PyStr(path.substringAfterLast('/'))
        }
        osPath.members["dirname"] = PyBuiltinFunction("dirname") { args, _, _ ->
            val path = getStr(args, 0, "dirname")
            val idx = path.lastIndexOf('/')
            PyStr(if (idx >= 0) path.substring(0, idx) else "")
        }
        os.members["path"] = osPath
        os.members["getcwd"] = PyBuiltinFunction("getcwd") { _, _, _ -> PyStr("/app/scripts") }
        os.members["listdir"] = PyBuiltinFunction("listdir") { _, _, _ ->
            val files = fileIO.listFiles().map { PyStr(it) as PyValue }.toMutableList()
            PyList(files)
        }
        modules["os"] = os

        // re
        val re = PyModule("re")
        re.members["findall"] = PyBuiltinFunction("findall") { args, _, _ ->
            val pattern = getStr(args, 0, "findall")
            val string = getStr(args, 1, "findall")
            val regex = Regex(pattern)
            val matches = regex.findAll(string).map { PyStr(it.value) as PyValue }.toMutableList()
            PyList(matches)
        }
        re.members["sub"] = PyBuiltinFunction("sub") { args, _, _ ->
            val pattern = getStr(args, 0, "sub")
            val repl = getStr(args, 1, "sub")
            val string = getStr(args, 2, "sub")
            PyStr(Regex(pattern).replace(string, repl))
        }
        re.members["split"] = PyBuiltinFunction("split") { args, _, _ ->
            val pattern = getStr(args, 0, "split")
            val string = getStr(args, 1, "split")
            val parts = Regex(pattern).split(string).map { PyStr(it) as PyValue }.toMutableList()
            PyList(parts)
        }
        modules["re"] = re

        return modules
    }

    private fun pyToJson(value: PyValue): String {
        return when (value) {
            is PyInt -> value.value.toString()
            is PyFloat -> value.value.toString()
            is PyStr -> "\"" + value.value.replace("\"", "\\\"").replace("\n", "\\n") + "\""
            is PyBool -> if (value.value) "true" else "false"
            is PyNone -> "null"
            is PyList -> "[${value.elements.joinToString(", ") { pyToJson(it) }}]"
            is PyTuple -> "[${value.elements.joinToString(", ") { pyToJson(it) }}]"
            is PyDict -> "{" + value.map.entries.joinToString(", ") {
                val k = if (it.key is PyStr) (it.key as PyStr).value else it.key.str()
                "\"$k\": ${pyToJson(it.value)}"
            } + "}"
            else -> "\"${value.str()}\""
        }
    }

    private fun jsonToPy(json: String): PyValue {
        if (json == "null") return PyNone
        if (json == "true") return PyBool(true)
        if (json == "false") return PyBool(false)
        json.toLongOrNull()?.let { return PyInt(it) }
        json.toDoubleOrNull()?.let { return PyFloat(it) }
        if (json.startsWith("\"") && json.endsWith("\"") && json.length >= 2) {
            return PyStr(json.substring(1, json.length - 1))
        }
        // Basic parser for simple arrays & objects
        if (json.startsWith("[") && json.endsWith("]")) {
            val inner = json.substring(1, json.length - 1).trim()
            if (inner.isEmpty()) return PyList(mutableListOf())
            val items = splitJson(inner)
            return PyList(items.map { jsonToPy(it.trim()) }.toMutableList())
        }
        if (json.startsWith("{") && json.endsWith("}")) {
            val inner = json.substring(1, json.length - 1).trim()
            val map = LinkedHashMap<PyValue, PyValue>()
            if (inner.isNotEmpty()) {
                val pairs = splitJson(inner)
                for (p in pairs) {
                    val idx = p.indexOf(':')
                    if (idx > 0) {
                        val k = p.substring(0, idx).trim().removeSurrounding("\"")
                        val v = p.substring(idx + 1).trim()
                        map[PyStr(k)] = jsonToPy(v)
                    }
                }
            }
            return PyDict(map)
        }
        return PyStr(json)
    }

    private fun splitJson(s: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inString = false
        val sb = StringBuilder()
        for (c in s) {
            if (c == '"' && (sb.isEmpty() || sb.last() != '\\')) inString = !inString
            if (!inString) {
                if (c == '[' || c == '{') depth++
                else if (c == ']' || c == '}') depth--
                else if (c == ',' && depth == 0) {
                    result.add(sb.toString())
                    sb.clear()
                    continue
                }
            }
            sb.append(c)
        }
        if (sb.isNotEmpty()) result.add(sb.toString())
        return result
    }

    fun getDouble(args: List<PyValue>, idx: Int, funcName: String): Double {
        val v = args.getOrNull(idx) ?: throw PyRuntimeError("TypeError", "$funcName() missing required argument at index $idx")
        return when (v) {
            is PyFloat -> v.value
            is PyInt -> v.value.toDouble()
            is PyBool -> if (v.value) 1.0 else 0.0
            else -> throw PyRuntimeError("TypeError", "must be real number, not ${v.typeName()}")
        }
    }

    fun getLong(args: List<PyValue>, idx: Int, funcName: String): Long {
        val v = args.getOrNull(idx) ?: throw PyRuntimeError("TypeError", "$funcName() missing required argument at index $idx")
        return when (v) {
            is PyInt -> v.value
            is PyFloat -> v.value.toLong()
            is PyBool -> if (v.value) 1L else 0L
            else -> throw PyRuntimeError("TypeError", "'${v.typeName()}' object cannot be interpreted as an integer")
        }
    }

    fun getStr(args: List<PyValue>, idx: Int, funcName: String): String {
        val v = args.getOrNull(idx) ?: throw PyRuntimeError("TypeError", "$funcName() missing required argument at index $idx")
        return when (v) {
            is PyStr -> v.value
            else -> throw PyRuntimeError("TypeError", "expected str, got ${v.typeName()}")
        }
    }
}
