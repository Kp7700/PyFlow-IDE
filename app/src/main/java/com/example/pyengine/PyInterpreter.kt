package com.example.pyengine

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow
import kotlin.math.roundToLong

class PyReturnSignal(val value: PyValue) : Exception()
class PyBreakSignal : Exception()
class PyContinueSignal : Exception()

class PyScope(
    val parent: PyScope? = null,
    val isGlobal: Boolean = parent == null
) {
    val variables = mutableMapOf<String, PyValue>()
    val globals = mutableSetOf<String>()
    val nonlocals = mutableSetOf<String>()

    fun get(name: String): PyValue? {
        if (variables.containsKey(name)) return variables[name]
        return parent?.get(name)
    }

    fun set(name: String, value: PyValue) {
        if (globals.contains(name)) {
            getGlobalScope().variables[name] = value
            return
        }
        if (nonlocals.contains(name)) {
            var curr = parent
            while (curr != null) {
                if (curr.variables.containsKey(name)) {
                    curr.variables[name] = value
                    return
                }
                curr = curr.parent
            }
        }
        variables[name] = value
    }

    fun getGlobalScope(): PyScope {
        var curr = this
        while (curr.parent != null) {
            curr = curr.parent!!
        }
        return curr
    }
}

class PyExecutionContext(
    val stdout: (String) -> Unit,
    val stderr: (String) -> Unit,
    val onInputPrompt: suspend (prompt: String) -> String,
    val coroutineContext: CoroutineContext,
    val fileIO: PyFileIO,
    val modules: MutableMap<String, PyModule> = mutableMapOf()
) {
    var currentLine: Int = 1
    var currentFile: String = "main.py"

    fun checkActive() {
        coroutineContext.ensureActive()
    }
}

class PyInterpreter(
    private val fileIO: PyFileIO,
    private val stdout: (String) -> Unit,
    private val stderr: (String) -> Unit,
    private val onInputPrompt: suspend (prompt: String) -> String
) {

    suspend fun execute(code: String, filename: String = "main.py") {
        val coroutineCtx = currentCoroutineContext()
        val execCtx = PyExecutionContext(
            stdout = stdout,
            stderr = stderr,
            onInputPrompt = onInputPrompt,
            coroutineContext = coroutineCtx,
            fileIO = fileIO
        )
        execCtx.currentFile = filename

        val globalScope = createGlobalScope(execCtx)
        val builtinModules = PyStandardLibrary.createBuiltinModules(fileIO)
        execCtx.modules.putAll(builtinModules)

        try {
            val lexer = PyLexer(code)
            val tokens = lexer.tokenize()
            val parser = PyParser(tokens)
            val stmts = parser.parse()

            execStatements(stmts, globalScope, execCtx)
        } catch (e: PyLexerException) {
            val errorMsg = "Traceback (most recent call last):\n  File \"$filename\", line ${e.line}\n    ${getCodeLine(code, e.line)}\nSyntaxError: ${e.message}"
            stderr(errorMsg)
            throw e
        } catch (e: PyParseException) {
            val errorMsg = "Traceback (most recent call last):\n  File \"$filename\", line ${e.line}\n    ${getCodeLine(code, e.line)}\n${e.message}"
            stderr(errorMsg)
            throw e
        } catch (e: PyRuntimeError) {
            val tbSb = StringBuilder("Traceback (most recent call last):\n")
            if (e.traceback.isNotEmpty()) {
                for (tb in e.traceback) {
                    tbSb.append("  $tb\n")
                }
            } else {
                val line = if (e.line > 0) e.line else execCtx.currentLine
                tbSb.append("  File \"$filename\", line $line\n")
                tbSb.append("    ${getCodeLine(code, line)}\n")
            }
            tbSb.append("${e.typeName}: ${e.message}")
            stderr(tbSb.toString())
            throw e
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                stderr("Execution stopped by user.")
                throw e
            }
            val errorMsg = "Traceback (most recent call last):\n  File \"$filename\", line ${execCtx.currentLine}\nRuntimeError: ${e.message ?: e.javaClass.simpleName}"
            stderr(errorMsg)
            throw e
        }
    }

    private fun getCodeLine(source: String, lineNum: Int): String {
        val lines = source.lines()
        return if (lineNum in 1..lines.size) lines[lineNum - 1].trim() else ""
    }

    private fun createGlobalScope(ctx: PyExecutionContext): PyScope {
        val scope = PyScope()

        // Standard Builtins
        scope.set("print", PyBuiltinFunction("print") { args, kwargs, _ ->
            val sep = (kwargs["sep"] as? PyStr)?.value ?: " "
            val end = (kwargs["end"] as? PyStr)?.value ?: "\n"
            val text = args.joinToString(sep) { it.str() } + end
            ctx.stdout(text)
            PyNone
        })

        scope.set("input", PyBuiltinFunction("input") { args, _, _ ->
            val prompt = if (args.isNotEmpty()) args[0].str() else ""
            if (prompt.isNotEmpty()) {
                ctx.stdout(prompt)
            }
            val userInput = ctx.onInputPrompt(prompt)
            PyStr(userInput)
        })

        scope.set("len", PyBuiltinFunction("len") { args, _, _ ->
            val obj = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "len() takes exactly one argument")
            when (obj) {
                is PyStr -> PyInt(obj.value.length.toLong())
                is PyList -> PyInt(obj.elements.size.toLong())
                is PyTuple -> PyInt(obj.elements.size.toLong())
                is PyDict -> PyInt(obj.map.size.toLong())
                is PySet -> PyInt(obj.set.size.toLong())
                is PyRange -> PyInt(obj.length())
                else -> throw PyRuntimeError("TypeError", "object of type '${obj.typeName()}' has no len()")
            }
        })

        scope.set("range", PyBuiltinFunction("range") { args, _, _ ->
            when (args.size) {
                1 -> PyRange(0, PyStandardLibrary.getLong(args, 0, "range"), 1)
                2 -> PyRange(PyStandardLibrary.getLong(args, 0, "range"), PyStandardLibrary.getLong(args, 1, "range"), 1)
                3 -> {
                    val step = PyStandardLibrary.getLong(args, 2, "range")
                    if (step == 0L) throw PyRuntimeError("ValueError", "range() arg 3 must not be zero")
                    PyRange(PyStandardLibrary.getLong(args, 0, "range"), PyStandardLibrary.getLong(args, 1, "range"), step)
                }
                else -> throw PyRuntimeError("TypeError", "range expected at most 3 arguments, got ${args.size}")
            }
        })

        scope.set("type", PyBuiltinFunction("type") { args, _, _ ->
            val obj = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "type() takes 1 argument")
            PyStr("<class '${obj.typeName()}'>")
        })

        scope.set("isinstance", PyBuiltinFunction("isinstance") { args, _, _ ->
            val obj = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "isinstance() missing arguments")
            val cls = args.getOrNull(1) ?: throw PyRuntimeError("TypeError", "isinstance() missing arguments")
            val isInstance = when {
                cls is PyClass && obj is PyInstance -> obj.pyClass == cls || obj.pyClass.bases.contains(cls)
                cls is PyStr -> obj.typeName() == cls.value
                else -> false
            }
            PyBool(isInstance)
        })

        scope.set("str", PyBuiltinFunction("str") { args, _, _ ->
            if (args.isEmpty()) PyStr("") else PyStr(args[0].str())
        })

        scope.set("int", PyBuiltinFunction("int") { args, _, _ ->
            if (args.isEmpty()) return@PyBuiltinFunction PyInt(0)
            val arg = args[0]
            val radix = if (args.size > 1) PyStandardLibrary.getLong(args, 1, "int").toInt() else 10
            when (arg) {
                is PyInt -> PyInt(arg.value)
                is PyFloat -> PyInt(arg.value.toLong())
                is PyBool -> PyInt(if (arg.value) 1 else 0)
                is PyStr -> {
                    val cleaned = arg.value.trim()
                    val v = cleaned.toLongOrNull(radix) ?: throw PyRuntimeError("ValueError", "invalid literal for int() with base $radix: '${arg.value}'")
                    PyInt(v)
                }
                else -> throw PyRuntimeError("TypeError", "int() argument must be a string or number, not '${arg.typeName()}'")
            }
        })

        scope.set("float", PyBuiltinFunction("float") { args, _, _ ->
            if (args.isEmpty()) return@PyBuiltinFunction PyFloat(0.0)
            val arg = args[0]
            when (arg) {
                is PyFloat -> PyFloat(arg.value)
                is PyInt -> PyFloat(arg.value.toDouble())
                is PyBool -> PyFloat(if (arg.value) 1.0 else 0.0)
                is PyStr -> {
                    val v = arg.value.trim().toDoubleOrNull() ?: throw PyRuntimeError("ValueError", "could not convert string to float: '${arg.value}'")
                    PyFloat(v)
                }
                else -> throw PyRuntimeError("TypeError", "float() argument must be a string or number, not '${arg.typeName()}'")
            }
        })

        scope.set("bool", PyBuiltinFunction("bool") { args, _, _ ->
            if (args.isEmpty()) PyBool(false) else PyBool(args[0].isTruthy())
        })

        scope.set("list", PyBuiltinFunction("list") { args, _, _ ->
            if (args.isEmpty()) return@PyBuiltinFunction PyList(mutableListOf())
            val seq = args[0]
            when (seq) {
                is PyList -> PyList(seq.elements.toMutableList())
                is PyTuple -> PyList(seq.elements.toMutableList())
                is PyStr -> PyList(seq.value.map { PyStr(it.toString()) }.toMutableList())
                is PySet -> PyList(seq.set.toMutableList())
                is PyRange -> PyList(seq.toList().toMutableList())
                is PyDict -> PyList(seq.map.keys.toMutableList())
                else -> throw PyRuntimeError("TypeError", "'${seq.typeName()}' object is not iterable")
            }
        })

        scope.set("tuple", PyBuiltinFunction("tuple") { args, _, _ ->
            if (args.isEmpty()) return@PyBuiltinFunction PyTuple(emptyList())
            val seq = args[0]
            when (seq) {
                is PyTuple -> seq
                is PyList -> PyTuple(seq.elements.toList())
                is PyStr -> PyTuple(seq.value.map { PyStr(it.toString()) })
                is PySet -> PyTuple(seq.set.toList())
                is PyRange -> PyTuple(seq.toList())
                is PyDict -> PyTuple(seq.map.keys.toList())
                else -> throw PyRuntimeError("TypeError", "'${seq.typeName()}' object is not iterable")
            }
        })

        scope.set("dict", PyBuiltinFunction("dict") { args, _, _ ->
            if (args.isEmpty()) return@PyBuiltinFunction PyDict(LinkedHashMap())
            val arg = args[0]
            if (arg is PyDict) return@PyBuiltinFunction PyDict(LinkedHashMap(arg.map))
            if (arg is PyList || arg is PyTuple) {
                val map = LinkedHashMap<PyValue, PyValue>()
                val items = if (arg is PyList) arg.elements else (arg as PyTuple).elements
                for (item in items) {
                    val pair = when (item) {
                        is PyTuple -> item.elements
                        is PyList -> item.elements
                        else -> throw PyRuntimeError("TypeError", "cannot convert dictionary update sequence element to a sequence")
                    }
                    if (pair.size != 2) throw PyRuntimeError("ValueError", "dictionary update sequence element has length ${pair.size}; 2 is required")
                    map[pair[0]] = pair[1]
                }
                return@PyBuiltinFunction PyDict(map)
            }
            throw PyRuntimeError("TypeError", "'${arg.typeName()}' object is not iterable")
        })

        scope.set("set", PyBuiltinFunction("set") { args, _, _ ->
            if (args.isEmpty()) return@PyBuiltinFunction PySet(LinkedHashSet())
            val seq = args[0]
            val set = LinkedHashSet<PyValue>()
            when (seq) {
                is PySet -> set.addAll(seq.set)
                is PyList -> set.addAll(seq.elements)
                is PyTuple -> set.addAll(seq.elements)
                is PyStr -> seq.value.forEach { set.add(PyStr(it.toString())) }
                is PyRange -> set.addAll(seq.toList())
                is PyDict -> set.addAll(seq.map.keys)
                else -> throw PyRuntimeError("TypeError", "'${seq.typeName()}' object is not iterable")
            }
            PySet(set)
        })

        scope.set("abs", PyBuiltinFunction("abs") { args, _, _ ->
            val v = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "abs() takes 1 argument")
            when (v) {
                is PyInt -> PyInt(kotlin.math.abs(v.value))
                is PyFloat -> PyFloat(kotlin.math.abs(v.value))
                is PyBool -> PyInt(if (v.value) 1 else 0)
                else -> throw PyRuntimeError("TypeError", "bad operand type for abs(): '${v.typeName()}'")
            }
        })

        scope.set("round", PyBuiltinFunction("round") { args, _, _ ->
            val num = PyStandardLibrary.getDouble(args, 0, "round")
            val digits = if (args.size > 1) PyStandardLibrary.getLong(args, 1, "round").toInt() else 0
            if (digits == 0) {
                PyInt(num.roundToLong())
            } else {
                val factor = 10.0.pow(digits)
                PyFloat(kotlin.math.round(num * factor) / factor)
            }
        })

        scope.set("min", PyBuiltinFunction("min") { args, _, _ ->
            val list = if (args.size == 1) getIterableElements(args[0]) else args
            if (list.isEmpty()) throw PyRuntimeError("ValueError", "min() arg is an empty sequence")
            list.minOrNull() ?: list[0]
        })

        scope.set("max", PyBuiltinFunction("max") { args, _, _ ->
            val list = if (args.size == 1) getIterableElements(args[0]) else args
            if (list.isEmpty()) throw PyRuntimeError("ValueError", "max() arg is an empty sequence")
            list.maxOrNull() ?: list[0]
        })

        scope.set("sum", PyBuiltinFunction("sum") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "sum() missing required argument")
            val list = getIterableElements(seq)
            var hasFloat = false
            var intSum = 0L
            var floatSum = 0.0
            for (item in list) {
                when (item) {
                    is PyInt -> { intSum += item.value; floatSum += item.value }
                    is PyFloat -> { hasFloat = true; floatSum += item.value }
                    is PyBool -> { val v = if (item.value) 1L else 0L; intSum += v; floatSum += v }
                    else -> throw PyRuntimeError("TypeError", "unsupported operand type(s) for +: '${item.typeName()}'")
                }
            }
            if (hasFloat) PyFloat(floatSum) else PyInt(intSum)
        })

        scope.set("sorted", PyBuiltinFunction("sorted") { args, kwargs, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "sorted() missing required argument")
            val list = getIterableElements(seq).toMutableList()
            val reverse = (kwargs["reverse"] as? PyBool)?.value ?: false
            list.sort()
            if (reverse) list.reverse()
            PyList(list)
        })

        scope.set("reversed", PyBuiltinFunction("reversed") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "reversed() missing required argument")
            val list = getIterableElements(seq).reversed().toMutableList()
            PyList(list)
        })

        scope.set("enumerate", PyBuiltinFunction("enumerate") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "enumerate() missing argument")
            val start = if (args.size > 1) PyStandardLibrary.getLong(args, 1, "enumerate") else 0L
            val list = getIterableElements(seq)
            val result = list.mapIndexed { idx, item -> PyTuple(listOf(PyInt(start + idx), item)) as PyValue }.toMutableList()
            PyList(result)
        })

        scope.set("zip", PyBuiltinFunction("zip") { args, _, _ ->
            if (args.isEmpty()) return@PyBuiltinFunction PyList(mutableListOf())
            val iterables = args.map { getIterableElements(it) }
            val minLen = iterables.minOfOrNull { it.size } ?: 0
            val result = mutableListOf<PyValue>()
            for (i in 0 until minLen) {
                val tuple = PyTuple(iterables.map { it[i] })
                result.add(tuple)
            }
            PyList(result)
        })

        scope.set("any", PyBuiltinFunction("any") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "any() missing argument")
            PyBool(getIterableElements(seq).any { it.isTruthy() })
        })

        scope.set("all", PyBuiltinFunction("all") { args, _, _ ->
            val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "all() missing argument")
            PyBool(getIterableElements(seq).all { it.isTruthy() })
        })

        scope.set("chr", PyBuiltinFunction("chr") { args, _, _ ->
            val n = PyStandardLibrary.getLong(args, 0, "chr")
            if (n < 0 || n > 0x10FFFF) throw PyRuntimeError("ValueError", "chr() arg not in range(0x110000)")
            PyStr(n.toInt().toChar().toString())
        })

        scope.set("ord", PyBuiltinFunction("ord") { args, _, _ ->
            val s = PyStandardLibrary.getStr(args, 0, "ord")
            if (s.length != 1) throw PyRuntimeError("TypeError", "ord() expected a character, but string of length ${s.length} found")
            PyInt(s[0].code.toLong())
        })

        scope.set("bin", PyBuiltinFunction("bin") { args, _, _ ->
            val n = PyStandardLibrary.getLong(args, 0, "bin")
            PyStr(if (n >= 0) "0b" + java.lang.Long.toBinaryString(n) else "-0b" + java.lang.Long.toBinaryString(-n))
        })

        scope.set("hex", PyBuiltinFunction("hex") { args, _, _ ->
            val n = PyStandardLibrary.getLong(args, 0, "hex")
            PyStr(if (n >= 0) "0x" + java.lang.Long.toHexString(n) else "-0x" + java.lang.Long.toHexString(-n))
        })

        scope.set("oct", PyBuiltinFunction("oct") { args, _, _ ->
            val n = PyStandardLibrary.getLong(args, 0, "oct")
            PyStr(if (n >= 0) "0o" + java.lang.Long.toOctalString(n) else "-0o" + java.lang.Long.toOctalString(-n))
        })

        scope.set("open", PyBuiltinFunction("open") { args, _, _ ->
            val filename = PyStandardLibrary.getStr(args, 0, "open")
            val mode = if (args.size > 1) PyStandardLibrary.getStr(args, 1, "open") else "r"
            fileIO.open(filename, mode)
        })

        scope.set("callable", PyBuiltinFunction("callable") { args, _, _ ->
            val obj = args.getOrNull(0) ?: PyNone
            val isCall = obj is PyFunction || obj is PyBuiltinFunction || obj is PyClass || obj is PyBoundMethod
            PyBool(isCall)
        })

        scope.set("dir", PyBuiltinFunction("dir") { args, _, _ ->
            val list = mutableListOf<PyValue>()
            if (args.isEmpty()) {
                scope.variables.keys.sorted().forEach { list.add(PyStr(it)) }
            } else {
                when (val obj = args[0]) {
                    is PyModule -> obj.members.keys.sorted().forEach { list.add(PyStr(it)) }
                    is PyClass -> obj.members.keys.sorted().forEach { list.add(PyStr(it)) }
                    is PyInstance -> {
                        val names = (obj.fields.keys + obj.pyClass.members.keys).sorted()
                        names.forEach { list.add(PyStr(it)) }
                    }
                    is PyStr -> listOf("count", "endswith", "find", "isdigit", "join", "lower", "replace", "split", "startswith", "strip", "upper").forEach { list.add(PyStr(it)) }
                    is PyList -> listOf("append", "clear", "copy", "count", "extend", "index", "insert", "pop", "remove", "reverse", "sort").forEach { list.add(PyStr(it)) }
                    is PyDict -> listOf("clear", "copy", "get", "items", "keys", "pop", "setdefault", "update", "values").forEach { list.add(PyStr(it)) }
                    is PySet -> listOf("add", "clear", "copy", "difference", "discard", "intersection", "pop", "remove", "union").forEach { list.add(PyStr(it)) }
                    else -> emptyList<String>()
                }
            }
            PyList(list)
        })

        return scope
    }

    private fun getIterableElements(value: PyValue): List<PyValue> {
        return when (value) {
            is PyList -> value.elements
            is PyTuple -> value.elements
            is PySet -> value.set.toList()
            is PyRange -> value.toList()
            is PyStr -> value.value.map { PyStr(it.toString()) }
            is PyDict -> value.map.keys.toList()
            else -> throw PyRuntimeError("TypeError", "'${value.typeName()}' object is not iterable")
        }
    }

    private suspend fun execStatements(stmts: List<Stmt>, scope: PyScope, ctx: PyExecutionContext) {
        for (stmt in stmts) {
            ctx.checkActive()
            ctx.currentLine = stmt.line
            execStatement(stmt, scope, ctx)
        }
    }

    private suspend fun execStatement(stmt: Stmt, scope: PyScope, ctx: PyExecutionContext) {
        when (stmt) {
            is FunctionDef -> {
                val defaultsMap = mutableMapOf<String, PyValue>()
                for ((p, defaultExpr) in stmt.defaults) {
                    defaultsMap[p] = evalExpr(defaultExpr, scope, ctx)
                }
                val func = PyFunction(
                    name = stmt.name,
                    params = stmt.params,
                    defaults = defaultsMap,
                    vararg = stmt.vararg,
                    kwarg = stmt.kwarg,
                    body = stmt.body,
                    closureScope = scope
                )
                scope.set(stmt.name, func)
            }
            is ClassDef -> {
                val baseClasses = mutableListOf<PyClass>()
                for (b in stmt.bases) {
                    val baseVal = scope.get(b) ?: throw PyRuntimeError("NameError", "name '$b' is not defined", stmt.line)
                    if (baseVal !is PyClass) throw PyRuntimeError("TypeError", "base class '$b' must be a class", stmt.line)
                    baseClasses.add(baseVal)
                }
                val classScope = PyScope(parent = scope)
                execStatements(stmt.body, classScope, ctx)
                val pyClass = PyClass(stmt.name, baseClasses, classScope.variables)
                scope.set(stmt.name, pyClass)
            }
            is IfStmt -> {
                var executed = false
                for ((condExpr, body) in stmt.branches) {
                    val cond = evalExpr(condExpr, scope, ctx)
                    if (cond.isTruthy()) {
                        execStatements(body, scope, ctx)
                        executed = true
                        break
                    }
                }
                if (!executed && stmt.elseBranch != null) {
                    execStatements(stmt.elseBranch, scope, ctx)
                }
            }
            is WhileStmt -> {
                var broke = false
                while (evalExpr(stmt.condition, scope, ctx).isTruthy()) {
                    ctx.checkActive()
                    try {
                        execStatements(stmt.body, scope, ctx)
                    } catch (e: PyBreakSignal) {
                        broke = true
                        break
                    } catch (e: PyContinueSignal) {
                        continue
                    }
                }
                if (!broke && stmt.elseBody != null) {
                    execStatements(stmt.elseBody, scope, ctx)
                }
            }
            is ForStmt -> {
                val iterVal = evalExpr(stmt.iter, scope, ctx)
                val elements = getIterableElements(iterVal)
                var broke = false
                for (elem in elements) {
                    ctx.checkActive()
                    assignTarget(stmt.target, elem, scope, ctx)
                    try {
                        execStatements(stmt.body, scope, ctx)
                    } catch (e: PyBreakSignal) {
                        broke = true
                        break
                    } catch (e: PyContinueSignal) {
                        continue
                    }
                }
                if (!broke && stmt.elseBody != null) {
                    execStatements(stmt.elseBody, scope, ctx)
                }
            }
            is TryStmt -> {
                var hadException = false
                try {
                    execStatements(stmt.body, scope, ctx)
                } catch (e: PyRuntimeError) {
                    hadException = true
                    var handled = false
                    for (h in stmt.handlers) {
                        if (h.type == null || h.type == e.typeName || h.type == "Exception" || h.type == "BaseException") {
                            handled = true
                            val tryScope = PyScope(parent = scope)
                            if (h.name != null) {
                                tryScope.set(h.name, PyStr(e.message))
                            }
                            execStatements(h.body, tryScope, ctx)
                            break
                        }
                    }
                    if (!handled) throw e
                } finally {
                    if (!hadException && stmt.elseBody != null) {
                        execStatements(stmt.elseBody, scope, ctx)
                    }
                    if (stmt.finallyBody != null) {
                        execStatements(stmt.finallyBody, scope, ctx)
                    }
                }
            }
            is WithStmt -> {
                val managers = mutableListOf<Pair<PyValue, PyValue>>()
                try {
                    for (item in stmt.items) {
                        val mgr = evalExpr(item.contextExpr, scope, ctx)
                        val enterVal = if (mgr is PyFile) {
                            mgr
                        } else if (mgr is PyInstance) {
                            val enterMethod = mgr.getAttr("__enter__")
                            if (enterMethod != null) {
                                callValue(enterMethod, emptyList(), emptyMap(), stmt.line, ctx)
                            } else {
                                mgr
                            }
                        } else {
                            mgr
                        }
                        managers.add(Pair(mgr, enterVal))
                        if (item.optionalVars != null) {
                            assignTarget(item.optionalVars, enterVal, scope, ctx)
                        }
                    }
                    execStatements(stmt.body, scope, ctx)
                } finally {
                    for ((mgr, _) in managers.reversed()) {
                        if (mgr is PyFile) {
                            mgr.isOpen = false
                            mgr.onSave?.invoke(mgr.filename, mgr.content.toString())
                        } else if (mgr is PyInstance) {
                            val exitMethod = mgr.getAttr("__exit__")
                            if (exitMethod != null) {
                                try {
                                    callValue(exitMethod, listOf(PyNone, PyNone, PyNone), emptyMap(), stmt.line, ctx)
                                } catch (ignored: Exception) {}
                            }
                        }
                    }
                }
            }
            is RaiseStmt -> {
                if (stmt.exc == null) throw PyRuntimeError("RuntimeError", "No active exception to reraise", stmt.line)
                val excVal = evalExpr(stmt.exc, scope, ctx)
                val msg = excVal.str()
                throw PyRuntimeError("Exception", msg, stmt.line)
            }
            is ReturnStmt -> {
                val retVal = if (stmt.value != null) evalExpr(stmt.value, scope, ctx) else PyNone
                throw PyReturnSignal(retVal)
            }
            is BreakStmt -> throw PyBreakSignal()
            is ContinueStmt -> throw PyContinueSignal()
            is PassStmt -> { /* no-op */ }
            is AssignStmt -> {
                val value = evalExpr(stmt.value, scope, ctx)
                for (target in stmt.targets) {
                    assignTarget(target, value, scope, ctx)
                }
            }
            is AugAssignStmt -> {
                val targetVal = evalExpr(stmt.target, scope, ctx)
                val rightVal = evalExpr(stmt.value, scope, ctx)
                val opName = stmt.op.removeSuffix("=")
                val result = applyBinOp(targetVal, opName, rightVal, stmt.line)
                assignTarget(stmt.target, result, scope, ctx)
            }
            is ExprStmt -> {
                evalExpr(stmt.expr, scope, ctx)
            }
            is ImportStmt -> {
                if (stmt.isFrom) {
                    val moduleName = stmt.fromModule ?: ""
                    val module = ctx.modules[moduleName] ?: throw PyRuntimeError("ModuleNotFoundError", "No module named '$moduleName'", stmt.line)
                    for ((name, alias) in stmt.names) {
                        val importedVal = module.members[name] ?: throw PyRuntimeError("ImportError", "cannot import name '$name' from '$moduleName'", stmt.line)
                        scope.set(alias ?: name, importedVal)
                    }
                } else {
                    for ((moduleName, alias) in stmt.names) {
                        val module = ctx.modules[moduleName] ?: throw PyRuntimeError("ModuleNotFoundError", "No module named '$moduleName'", stmt.line)
                        scope.set(alias ?: moduleName, module)
                    }
                }
            }
            is GlobalStmt -> {
                for (name in stmt.names) {
                    scope.globals.add(name)
                }
            }
            is NonlocalStmt -> {
                for (name in stmt.names) {
                    scope.nonlocals.add(name)
                }
            }
            is AssertStmt -> {
                val test = evalExpr(stmt.test, scope, ctx)
                if (!test.isTruthy()) {
                    val msg = if (stmt.msg != null) evalExpr(stmt.msg, scope, ctx).str() else ""
                    throw PyRuntimeError("AssertionError", msg, stmt.line)
                }
            }
        }
    }

    private suspend fun assignTarget(target: Expr, value: PyValue, scope: PyScope, ctx: PyExecutionContext) {
        when (target) {
            is NameExpr -> {
                scope.set(target.name, value)
            }
            is AttributeExpr -> {
                val obj = evalExpr(target.value, scope, ctx)
                if (obj is PyInstance) {
                    obj.setAttr(target.attr, value)
                } else if (obj is PyModule) {
                    obj.members[target.attr] = value
                } else {
                    throw PyRuntimeError("AttributeError", "'${obj.typeName()}' object has no attribute '${target.attr}'", target.line)
                }
            }
            is SubscriptExpr -> {
                val obj = evalExpr(target.value, scope, ctx)
                if (!target.slice.isSingleIndex || target.slice.lower == null) {
                    throw PyRuntimeError("TypeError", "slice assignment is not supported", target.line)
                }
                val indexVal = evalExpr(target.slice.lower, scope, ctx)
                when (obj) {
                    is PyList -> {
                        val idx = getValidIndex(indexVal, obj.elements.size, target.line)
                        obj.elements[idx] = value
                    }
                    is PyDict -> {
                        obj.map[indexVal] = value
                    }
                    else -> throw PyRuntimeError("TypeError", "'${obj.typeName()}' object does not support item assignment", target.line)
                }
            }
            is TupleExpr, is ListExpr -> {
                val targets = if (target is TupleExpr) target.elements else (target as ListExpr).elements
                val values = getIterableElements(value)
                if (targets.size != values.size) {
                    throw PyRuntimeError("ValueError", "too many values to unpack (expected ${targets.size}, got ${values.size})", target.line)
                }
                for (i in targets.indices) {
                    assignTarget(targets[i], values[i], scope, ctx)
                }
            }
            else -> throw PyRuntimeError("SyntaxError", "cannot assign to expression", target.line)
        }
    }

    suspend fun evalExpr(expr: Expr, scope: PyScope, ctx: PyExecutionContext): PyValue {
        ctx.checkActive()
        return when (expr) {
            is IntLiteralExpr -> PyInt(expr.value)
            is FloatLiteralExpr -> PyFloat(expr.value)
            is StringLiteralExpr -> PyStr(expr.value)
            is BoolLiteralExpr -> PyBool(expr.value)
            is NoneLiteralExpr -> PyNone
            is NameExpr -> {
                scope.get(expr.name) ?: throw PyRuntimeError("NameError", "name '${expr.name}' is not defined", expr.line)
            }
            is BinOp -> {
                val left = evalExpr(expr.left, scope, ctx)
                val right = evalExpr(expr.right, scope, ctx)
                applyBinOp(left, expr.op, right, expr.line)
            }
            is UnaryOp -> {
                val operand = evalExpr(expr.operand, scope, ctx)
                when (expr.op) {
                    "+" -> {
                        if (operand is PyInt || operand is PyFloat) operand
                        else throw PyRuntimeError("TypeError", "bad operand type for unary +: '${operand.typeName()}'", expr.line)
                    }
                    "-" -> {
                        when (operand) {
                            is PyInt -> PyInt(-operand.value)
                            is PyFloat -> PyFloat(-operand.value)
                            is PyBool -> PyInt(if (operand.value) -1 else 0)
                            else -> throw PyRuntimeError("TypeError", "bad operand type for unary -: '${operand.typeName()}'", expr.line)
                        }
                    }
                    "~" -> {
                        if (operand is PyInt) PyInt(operand.value.inv())
                        else throw PyRuntimeError("TypeError", "bad operand type for unary ~: '${operand.typeName()}'", expr.line)
                    }
                    "not" -> PyBool(!operand.isTruthy())
                    else -> throw PyRuntimeError("SyntaxError", "Unknown unary op '${expr.op}'", expr.line)
                }
            }
            is CompareOp -> {
                var currentLeft = evalExpr(expr.left, scope, ctx)
                for ((op, rightExpr) in expr.comparisons) {
                    val currentRight = evalExpr(rightExpr, scope, ctx)
                    val passes = applyComparison(currentLeft, op, currentRight, expr.line)
                    if (!passes) return PyBool(false)
                    currentLeft = currentRight
                }
                PyBool(true)
            }
            is BoolOp -> {
                if (expr.op == "and") {
                    var lastVal: PyValue = PyBool(true)
                    for (vExpr in expr.values) {
                        lastVal = evalExpr(vExpr, scope, ctx)
                        if (!lastVal.isTruthy()) return lastVal
                    }
                    lastVal
                } else { // "or"
                    var lastVal: PyValue = PyBool(false)
                    for (vExpr in expr.values) {
                        lastVal = evalExpr(vExpr, scope, ctx)
                        if (lastVal.isTruthy()) return lastVal
                    }
                    lastVal
                }
            }
            is CallExpr -> {
                val callee = evalExpr(expr.callee, scope, ctx)
                val evaluatedArgs = expr.args.map { evalExpr(it, scope, ctx) }
                val evaluatedKwargs = expr.kwargs.associate { it.first to evalExpr(it.second, scope, ctx) }
                callValue(callee, evaluatedArgs, evaluatedKwargs, expr.line, ctx)
            }
            is AttributeExpr -> {
                val obj = evalExpr(expr.value, scope, ctx)
                getMemberOrMethod(obj, expr.attr, expr.line)
            }
            is SubscriptExpr -> {
                val obj = evalExpr(expr.value, scope, ctx)
                evalSubscript(obj, expr.slice, scope, ctx)
            }
            is ListExpr -> {
                val elements = expr.elements.map { evalExpr(it, scope, ctx) }.toMutableList()
                PyList(elements)
            }
            is TupleExpr -> {
                val elements = expr.elements.map { evalExpr(it, scope, ctx) }
                PyTuple(elements)
            }
            is DictExpr -> {
                val map = LinkedHashMap<PyValue, PyValue>()
                for ((kExpr, vExpr) in expr.entries) {
                    val k = evalExpr(kExpr, scope, ctx)
                    val v = evalExpr(vExpr, scope, ctx)
                    map[k] = v
                }
                PyDict(map)
            }
            is SetExpr -> {
                val set = LinkedHashSet<PyValue>()
                for (elemExpr in expr.elements) {
                    set.add(evalExpr(elemExpr, scope, ctx))
                }
                PySet(set)
            }
            is ListCompExpr -> {
                val result = mutableListOf<PyValue>()
                val iterVal = evalExpr(expr.iter, scope, ctx)
                val elements = getIterableElements(iterVal)
                val compScope = PyScope(parent = scope)
                for (elem in elements) {
                    assignTarget(expr.target, elem, compScope, ctx)
                    var include = true
                    for (ifCond in expr.ifs) {
                        if (!evalExpr(ifCond, compScope, ctx).isTruthy()) {
                            include = false
                            break
                        }
                    }
                    if (include) {
                        result.add(evalExpr(expr.elt, compScope, ctx))
                    }
                }
                PyList(result)
            }
            is DictCompExpr -> {
                val map = LinkedHashMap<PyValue, PyValue>()
                val iterVal = evalExpr(expr.iter, scope, ctx)
                val elements = getIterableElements(iterVal)
                val compScope = PyScope(parent = scope)
                for (elem in elements) {
                    assignTarget(expr.target, elem, compScope, ctx)
                    var include = true
                    for (ifCond in expr.ifs) {
                        if (!evalExpr(ifCond, compScope, ctx).isTruthy()) {
                            include = false
                            break
                        }
                    }
                    if (include) {
                        val k = evalExpr(expr.key, compScope, ctx)
                        val v = evalExpr(expr.value, compScope, ctx)
                        map[k] = v
                    }
                }
                PyDict(map)
            }
            is SetCompExpr -> {
                val set = LinkedHashSet<PyValue>()
                val iterVal = evalExpr(expr.iter, scope, ctx)
                val elements = getIterableElements(iterVal)
                val compScope = PyScope(parent = scope)
                for (elem in elements) {
                    assignTarget(expr.target, elem, compScope, ctx)
                    var include = true
                    for (ifCond in expr.ifs) {
                        if (!evalExpr(ifCond, compScope, ctx).isTruthy()) {
                            include = false
                            break
                        }
                    }
                    if (include) {
                        set.add(evalExpr(expr.elt, compScope, ctx))
                    }
                }
                PySet(set)
            }
            is LambdaExpr -> {
                val defaultsMap = mutableMapOf<String, PyValue>()
                for ((p, defExpr) in expr.defaults) {
                    defaultsMap[p] = evalExpr(defExpr, scope, ctx)
                }
                PyFunction(
                    name = "<lambda>",
                    params = expr.params,
                    defaults = defaultsMap,
                    vararg = null,
                    kwarg = null,
                    body = listOf(ReturnStmt(expr.body, expr.line)),
                    closureScope = scope
                )
            }
            is TernaryExpr -> {
                val cond = evalExpr(expr.condition, scope, ctx)
                if (cond.isTruthy()) evalExpr(expr.trueExpr, scope, ctx) else evalExpr(expr.falseExpr, scope, ctx)
            }
            is FStringExpr -> {
                val sb = StringBuilder()
                for (part in expr.parts) {
                    when (part) {
                        is String -> sb.append(part)
                        is Expr -> sb.append(evalExpr(part, scope, ctx).str())
                        else -> sb.append(part.toString())
                    }
                }
                PyStr(sb.toString())
            }
            is SliceExpr -> throw PyRuntimeError("SyntaxError", "Invalid slice expression", expr.line)
        }
    }

    private suspend fun evalSubscript(obj: PyValue, slice: SliceExpr, scope: PyScope, ctx: PyExecutionContext): PyValue {
        if (slice.isSingleIndex && slice.lower != null) {
            val idxVal = evalExpr(slice.lower, scope, ctx)
            return when (obj) {
                is PyList -> {
                    val idx = getValidIndex(idxVal, obj.elements.size, slice.line)
                    obj.elements[idx]
                }
                is PyTuple -> {
                    val idx = getValidIndex(idxVal, obj.elements.size, slice.line)
                    obj.elements[idx]
                }
                is PyStr -> {
                    val idx = getValidIndex(idxVal, obj.value.length, slice.line)
                    PyStr(obj.value[idx].toString())
                }
                is PyDict -> {
                    obj.map[idxVal] ?: throw PyRuntimeError("KeyError", idxVal.repr(), slice.line)
                }
                else -> throw PyRuntimeError("TypeError", "'${obj.typeName()}' object is not subscriptable", slice.line)
            }
        }

        // Slicing [lower:upper:step]
        val step = if (slice.step != null) PyStandardLibrary.getLong(listOf(evalExpr(slice.step, scope, ctx)), 0, "slice") else 1L
        if (step == 0L) throw PyRuntimeError("ValueError", "slice step cannot be zero", slice.line)

        return when (obj) {
            is PyList -> {
                val bounds = computeSliceBounds(slice.lower?.let { evalExpr(it, scope, ctx) }, slice.upper?.let { evalExpr(it, scope, ctx) }, step, obj.elements.size)
                val sliced = extractSlice(obj.elements, bounds.first, bounds.second, step)
                PyList(sliced.toMutableList())
            }
            is PyTuple -> {
                val bounds = computeSliceBounds(slice.lower?.let { evalExpr(it, scope, ctx) }, slice.upper?.let { evalExpr(it, scope, ctx) }, step, obj.elements.size)
                val sliced = extractSlice(obj.elements, bounds.first, bounds.second, step)
                PyTuple(sliced)
            }
            is PyStr -> {
                val bounds = computeSliceBounds(slice.lower?.let { evalExpr(it, scope, ctx) }, slice.upper?.let { evalExpr(it, scope, ctx) }, step, obj.value.length)
                val sliced = extractSlice(obj.value.toList(), bounds.first, bounds.second, step)
                PyStr(sliced.joinToString(""))
            }
            else -> throw PyRuntimeError("TypeError", "'${obj.typeName()}' object is not subscriptable", slice.line)
        }
    }

    private fun computeSliceBounds(lowerVal: PyValue?, upperVal: PyValue?, step: Long, len: Int): Pair<Int, Int> {
        val start = if (lowerVal != null) {
            var s = (lowerVal as? PyInt)?.value?.toInt() ?: 0
            if (s < 0) s += len
            s.coerceIn(0, len)
        } else {
            if (step > 0) 0 else len - 1
        }

        val stop = if (upperVal != null) {
            var e = (upperVal as? PyInt)?.value?.toInt() ?: len
            if (e < 0) e += len
            e.coerceIn(0, len)
        } else {
            if (step > 0) len else -1
        }

        return Pair(start, stop)
    }

    private fun <T> extractSlice(list: List<T>, start: Int, stop: Int, step: Long): List<T> {
        val result = mutableListOf<T>()
        var curr = start
        if (step > 0) {
            while (curr < stop && curr in list.indices) {
                result.add(list[curr])
                curr += step.toInt()
            }
        } else {
            while (curr > stop && curr in list.indices) {
                result.add(list[curr])
                curr += step.toInt()
            }
        }
        return result
    }

    private fun getValidIndex(idxVal: PyValue, size: Int, line: Int): Int {
        if (idxVal !is PyInt) throw PyRuntimeError("TypeError", "indices must be integers, not ${idxVal.typeName()}", line)
        var idx = idxVal.value.toInt()
        if (idx < 0) idx += size
        if (idx !in 0 until size) throw PyRuntimeError("IndexError", "list index out of range", line)
        return idx
    }

    private suspend fun callValue(
        callee: PyValue,
        args: List<PyValue>,
        kwargs: Map<String, PyValue>,
        line: Int,
        ctx: PyExecutionContext
    ): PyValue {
        when (callee) {
            is PyBuiltinFunction -> {
                return callee.func(args, kwargs, ctx)
            }
            is PyFunction -> {
                val callScope = PyScope(parent = callee.closureScope)
                bindFunctionArgs(callee, args, kwargs, callScope, line)
                return try {
                    execStatements(callee.body, callScope, ctx)
                    PyNone
                } catch (r: PyReturnSignal) {
                    r.value
                }
            }
            is PyBoundMethod -> {
                val combinedArgs = listOf(callee.instance) + args
                val callScope = PyScope(parent = callee.function.closureScope)
                bindFunctionArgs(callee.function, combinedArgs, kwargs, callScope, line)
                return try {
                    execStatements(callee.function.body, callScope, ctx)
                    PyNone
                } catch (r: PyReturnSignal) {
                    r.value
                }
            }
            is PyClass -> {
                val instance = PyInstance(callee)
                val initMethod = instance.getAttr("__init__")
                if (initMethod is PyBoundMethod) {
                    callValue(initMethod, args, kwargs, line, ctx)
                }
                return instance
            }
            else -> throw PyRuntimeError("TypeError", "'${callee.typeName()}' object is not callable", line)
        }
    }

    private fun bindFunctionArgs(
        func: PyFunction,
        args: List<PyValue>,
        kwargs: Map<String, PyValue>,
        scope: PyScope,
        line: Int
    ) {
        var argIdx = 0
        val remainingKwargs = kwargs.toMutableMap()

        for (param in func.params) {
            if (argIdx < args.size) {
                scope.set(param, args[argIdx++])
            } else if (remainingKwargs.containsKey(param)) {
                scope.set(param, remainingKwargs.remove(param)!!)
            } else if (func.defaults.containsKey(param)) {
                scope.set(param, func.defaults[param]!!)
            } else {
                throw PyRuntimeError("TypeError", "${func.name}() missing required positional argument: '$param'", line)
            }
        }

        // *args
        if (func.vararg != null) {
            val varargsList = if (argIdx < args.size) args.subList(argIdx, args.size) else emptyList()
            scope.set(func.vararg, PyTuple(varargsList))
        }

        // **kwargs
        if (func.kwarg != null) {
            val kwDict = LinkedHashMap<PyValue, PyValue>()
            for ((k, v) in remainingKwargs) {
                kwDict[PyStr(k)] = v
            }
            scope.set(func.kwarg, PyDict(kwDict))
        }
    }

    private fun getMemberOrMethod(obj: PyValue, attr: String, line: Int): PyValue {
        when (obj) {
            is PyInstance -> {
                return obj.getAttr(attr) ?: throw PyRuntimeError("AttributeError", "'${obj.pyClass.name}' object has no attribute '$attr'", line)
            }
            is PyClass -> {
                return obj.findMember(attr) ?: throw PyRuntimeError("AttributeError", "type object '${obj.name}' has no attribute '$attr'", line)
            }
            is PyModule -> {
                return obj.members[attr] ?: throw PyRuntimeError("AttributeError", "module '${obj.name}' has no attribute '$attr'", line)
            }
            is PyStr -> {
                return getStringMethod(obj, attr)
            }
            is PyList -> {
                return getListMethod(obj, attr)
            }
            is PyDict -> {
                return getDictMethod(obj, attr)
            }
            is PySet -> {
                return getSetMethod(obj, attr)
            }
            is PyFile -> {
                return getFileMethod(obj, attr)
            }
            else -> throw PyRuntimeError("AttributeError", "'${obj.typeName()}' object has no attribute '$attr'", line)
        }
    }

    private fun getStringMethod(strObj: PyStr, attr: String): PyValue {
        val s = strObj.value
        return when (attr) {
            "lower" -> PyBuiltinFunction("lower") { _, _, _ -> PyStr(s.lowercase()) }
            "upper" -> PyBuiltinFunction("upper") { _, _, _ -> PyStr(s.uppercase()) }
            "strip" -> PyBuiltinFunction("strip") { args, _, _ ->
                val chars = if (args.isNotEmpty()) PyStandardLibrary.getStr(args, 0, "strip") else null
                PyStr(if (chars == null) s.trim() else s.trim { it in chars })
            }
            "split" -> PyBuiltinFunction("split") { args, _, _ ->
                val sep = if (args.isNotEmpty()) PyStandardLibrary.getStr(args, 0, "split") else null
                val parts = if (sep == null) s.trim().split(Regex("\\s+")) else s.split(sep)
                PyList(parts.map { PyStr(it) }.toMutableList())
            }
            "join" -> PyBuiltinFunction("join") { args, _, _ ->
                val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "join() missing iterable")
                val items = getIterableElements(seq).map { (it as? PyStr)?.value ?: it.str() }
                PyStr(items.joinToString(s))
            }
            "replace" -> PyBuiltinFunction("replace") { args, _, _ ->
                val oldS = PyStandardLibrary.getStr(args, 0, "replace")
                val newS = PyStandardLibrary.getStr(args, 1, "replace")
                PyStr(s.replace(oldS, newS))
            }
            "startswith" -> PyBuiltinFunction("startswith") { args, _, _ ->
                val prefix = PyStandardLibrary.getStr(args, 0, "startswith")
                PyBool(s.startsWith(prefix))
            }
            "endswith" -> PyBuiltinFunction("endswith") { args, _, _ ->
                val suffix = PyStandardLibrary.getStr(args, 0, "endswith")
                PyBool(s.endsWith(suffix))
            }
            "find" -> PyBuiltinFunction("find") { args, _, _ ->
                val sub = PyStandardLibrary.getStr(args, 0, "find")
                PyInt(s.indexOf(sub).toLong())
            }
            "count" -> PyBuiltinFunction("count") { args, _, _ ->
                val sub = PyStandardLibrary.getStr(args, 0, "count")
                var count = 0L
                var idx = 0
                while (idx != -1) {
                    idx = s.indexOf(sub, idx)
                    if (idx != -1) {
                        count++
                        idx += sub.length
                    }
                }
                PyInt(count)
            }
            "isdigit" -> PyBuiltinFunction("isdigit") { _, _, _ -> PyBool(s.isNotEmpty() && s.all { it.isDigit() }) }
            "isalpha" -> PyBuiltinFunction("isalpha") { _, _, _ -> PyBool(s.isNotEmpty() && s.all { it.isLetter() }) }
            "title" -> PyBuiltinFunction("title") { _, _, _ -> PyStr(s.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }) }
            else -> throw PyRuntimeError("AttributeError", "'str' object has no attribute '$attr'")
        }
    }

    private fun getListMethod(listObj: PyList, attr: String): PyValue {
        val list = listObj.elements
        return when (attr) {
            "append" -> PyBuiltinFunction("append") { args, _, _ ->
                val item = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "append() missing 1 argument")
                list.add(item)
                PyNone
            }
            "extend" -> PyBuiltinFunction("extend") { args, _, _ ->
                val seq = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "extend() missing argument")
                list.addAll(getIterableElements(seq))
                PyNone
            }
            "insert" -> PyBuiltinFunction("insert") { args, _, _ ->
                val idx = PyStandardLibrary.getLong(args, 0, "insert").toInt()
                val item = args.getOrNull(1) ?: throw PyRuntimeError("TypeError", "insert() missing argument")
                val clamped = idx.coerceIn(0, list.size)
                list.add(clamped, item)
                PyNone
            }
            "pop" -> PyBuiltinFunction("pop") { args, _, _ ->
                if (list.isEmpty()) throw PyRuntimeError("IndexError", "pop from empty list")
                val idx = if (args.isNotEmpty()) PyStandardLibrary.getLong(args, 0, "pop").toInt() else list.size - 1
                val realIdx = if (idx < 0) idx + list.size else idx
                if (realIdx !in list.indices) throw PyRuntimeError("IndexError", "pop index out of range")
                list.removeAt(realIdx)
            }
            "remove" -> PyBuiltinFunction("remove") { args, _, _ ->
                val item = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "remove() missing argument")
                val found = list.remove(item)
                if (!found) throw PyRuntimeError("ValueError", "list.remove(x): x not in list")
                PyNone
            }
            "clear" -> PyBuiltinFunction("clear") { _, _, _ ->
                list.clear()
                PyNone
            }
            "index" -> PyBuiltinFunction("index") { args, _, _ ->
                val item = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "index() missing argument")
                val idx = list.indexOf(item)
                if (idx == -1) throw PyRuntimeError("ValueError", "${item.repr()} is not in list")
                PyInt(idx.toLong())
            }
            "count" -> PyBuiltinFunction("count") { args, _, _ ->
                val item = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "count() missing argument")
                PyInt(list.count { it == item }.toLong())
            }
            "reverse" -> PyBuiltinFunction("reverse") { _, _, _ ->
                list.reverse()
                PyNone
            }
            "sort" -> PyBuiltinFunction("sort") { _, kwargs, _ ->
                val reverse = (kwargs["reverse"] as? PyBool)?.value ?: false
                list.sort()
                if (reverse) list.reverse()
                PyNone
            }
            "copy" -> PyBuiltinFunction("copy") { _, _, _ ->
                PyList(list.toMutableList())
            }
            else -> throw PyRuntimeError("AttributeError", "'list' object has no attribute '$attr'")
        }
    }

    private fun getDictMethod(dictObj: PyDict, attr: String): PyValue {
        val map = dictObj.map
        return when (attr) {
            "keys" -> PyBuiltinFunction("keys") { _, _, _ -> PyList(map.keys.toMutableList()) }
            "values" -> PyBuiltinFunction("values") { _, _, _ -> PyList(map.values.toMutableList()) }
            "items" -> PyBuiltinFunction("items") { _, _, _ ->
                val list = map.entries.map { PyTuple(listOf(it.key, it.value)) as PyValue }.toMutableList()
                PyList(list)
            }
            "get" -> PyBuiltinFunction("get") { args, _, _ ->
                val key = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "get() missing argument")
                val defaultVal = args.getOrNull(1) ?: PyNone
                map[key] ?: defaultVal
            }
            "pop" -> PyBuiltinFunction("pop") { args, _, _ ->
                val key = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "pop() missing argument")
                if (map.containsKey(key)) {
                    map.remove(key)!!
                } else if (args.size > 1) {
                    args[1]
                } else {
                    throw PyRuntimeError("KeyError", key.repr())
                }
            }
            "clear" -> PyBuiltinFunction("clear") { _, _, _ ->
                map.clear()
                PyNone
            }
            "update" -> PyBuiltinFunction("update") { args, _, _ ->
                val other = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "update() missing argument")
                if (other is PyDict) {
                    map.putAll(other.map)
                }
                PyNone
            }
            "setdefault" -> PyBuiltinFunction("setdefault") { args, _, _ ->
                val key = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "setdefault() missing argument")
                val defaultVal = args.getOrNull(1) ?: PyNone
                if (!map.containsKey(key)) {
                    map[key] = defaultVal
                }
                map[key]!!
            }
            "copy" -> PyBuiltinFunction("copy") { _, _, _ ->
                PyDict(LinkedHashMap(map))
            }
            else -> throw PyRuntimeError("AttributeError", "'dict' object has no attribute '$attr'")
        }
    }

    private fun getSetMethod(setObj: PySet, attr: String): PyValue {
        val set = setObj.set
        return when (attr) {
            "add" -> PyBuiltinFunction("add") { args, _, _ ->
                val elem = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "add() missing argument")
                set.add(elem)
                PyNone
            }
            "remove" -> PyBuiltinFunction("remove") { args, _, _ ->
                val elem = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "remove() missing argument")
                if (!set.remove(elem)) throw PyRuntimeError("KeyError", elem.repr())
                PyNone
            }
            "discard" -> PyBuiltinFunction("discard") { args, _, _ ->
                val elem = args.getOrNull(0) ?: throw PyRuntimeError("TypeError", "discard() missing argument")
                set.remove(elem)
                PyNone
            }
            "pop" -> PyBuiltinFunction("pop") { _, _, _ ->
                val elem = set.firstOrNull() ?: throw PyRuntimeError("KeyError", "pop from an empty set")
                set.remove(elem)
                elem
            }
            "clear" -> PyBuiltinFunction("clear") { _, _, _ ->
                set.clear()
                PyNone
            }
            "union" -> PyBuiltinFunction("union") { args, _, _ ->
                val newSet = LinkedHashSet(set)
                for (arg in args) {
                    newSet.addAll(getIterableElements(arg))
                }
                PySet(newSet)
            }
            "intersection" -> PyBuiltinFunction("intersection") { args, _, _ ->
                val newSet = LinkedHashSet(set)
                for (arg in args) {
                    newSet.retainAll(getIterableElements(arg).toSet())
                }
                PySet(newSet)
            }
            "difference" -> PyBuiltinFunction("difference") { args, _, _ ->
                val newSet = LinkedHashSet(set)
                for (arg in args) {
                    newSet.removeAll(getIterableElements(arg).toSet())
                }
                PySet(newSet)
            }
            "copy" -> PyBuiltinFunction("copy") { _, _, _ ->
                PySet(LinkedHashSet(set))
            }
            else -> throw PyRuntimeError("AttributeError", "'set' object has no attribute '$attr'")
        }
    }

    private fun getFileMethod(fileObj: PyFile, attr: String): PyValue {
        return when (attr) {
            "read" -> PyBuiltinFunction("read") { args, _, _ ->
                if (!fileObj.isOpen) throw PyRuntimeError("ValueError", "I/O operation on closed file.")
                val size = if (args.isNotEmpty()) PyStandardLibrary.getLong(args, 0, "read").toInt() else -1
                val text = fileObj.content.toString()
                val result = if (size < 0) {
                    val r = text.substring(fileObj.position.coerceAtMost(text.length))
                    fileObj.position = text.length
                    r
                } else {
                    val end = (fileObj.position + size).coerceAtMost(text.length)
                    val r = text.substring(fileObj.position, end)
                    fileObj.position = end
                    r
                }
                PyStr(result)
            }
            "readline" -> PyBuiltinFunction("readline") { _, _, _ ->
                if (!fileObj.isOpen) throw PyRuntimeError("ValueError", "I/O operation on closed file.")
                val text = fileObj.content.toString()
                if (fileObj.position >= text.length) return@PyBuiltinFunction PyStr("")
                val newlineIdx = text.indexOf('\n', fileObj.position)
                val line = if (newlineIdx == -1) {
                    val l = text.substring(fileObj.position)
                    fileObj.position = text.length
                    l
                } else {
                    val l = text.substring(fileObj.position, newlineIdx + 1)
                    fileObj.position = newlineIdx + 1
                    l
                }
                PyStr(line)
            }
            "readlines" -> PyBuiltinFunction("readlines") { _, _, _ ->
                if (!fileObj.isOpen) throw PyRuntimeError("ValueError", "I/O operation on closed file.")
                val text = fileObj.content.toString()
                val lines = text.substring(fileObj.position.coerceAtMost(text.length)).lines().map { PyStr(it + "\n") as PyValue }.toMutableList()
                fileObj.position = text.length
                PyList(lines)
            }
            "write" -> PyBuiltinFunction("write") { args, _, _ ->
                if (!fileObj.isOpen) throw PyRuntimeError("ValueError", "I/O operation on closed file.")
                val data = PyStandardLibrary.getStr(args, 0, "write")
                fileObj.content.append(data)
                fileObj.onSave?.invoke(fileObj.filename, fileObj.content.toString())
                PyInt(data.length.toLong())
            }
            "close" -> PyBuiltinFunction("close") { _, _, _ ->
                fileObj.isOpen = false
                fileObj.onSave?.invoke(fileObj.filename, fileObj.content.toString())
                PyNone
            }
            "__enter__" -> PyBuiltinFunction("__enter__") { _, _, _ -> fileObj }
            "__exit__" -> PyBuiltinFunction("__exit__") { _, _, _ ->
                fileObj.isOpen = false
                fileObj.onSave?.invoke(fileObj.filename, fileObj.content.toString())
                PyNone
            }
            else -> throw PyRuntimeError("AttributeError", "'_io.TextIOWrapper' object has no attribute '$attr'")
        }
    }

    private fun applyBinOp(left: PyValue, op: String, right: PyValue, line: Int): PyValue {
        // String formatting '%'
        if (op == "%" && left is PyStr) {
            return formatString(left.value, right)
        }

        // Int + Int
        if (left is PyInt && right is PyInt) {
            return when (op) {
                "+" -> PyInt(left.value + right.value)
                "-" -> PyInt(left.value - right.value)
                "*" -> PyInt(left.value * right.value)
                "/" -> {
                    if (right.value == 0L) throw PyRuntimeError("ZeroDivisionError", "division by zero", line)
                    PyFloat(left.value.toDouble() / right.value.toDouble())
                }
                "//" -> {
                    if (right.value == 0L) throw PyRuntimeError("ZeroDivisionError", "integer division or modulo by zero", line)
                    PyInt(Math.floorDiv(left.value, right.value))
                }
                "%" -> {
                    if (right.value == 0L) throw PyRuntimeError("ZeroDivisionError", "integer division or modulo by zero", line)
                    PyInt(Math.floorMod(left.value, right.value))
                }
                "**" -> {
                    if (right.value >= 0) {
                        var res = 1L
                        for (i in 0 until right.value) res *= left.value
                        PyInt(res)
                    } else {
                        PyFloat(left.value.toDouble().pow(right.value.toDouble()))
                    }
                }
                "&" -> PyInt(left.value and right.value)
                "|" -> PyInt(left.value or right.value)
                "^" -> PyInt(left.value xor right.value)
                "<<" -> PyInt(left.value shl right.value.toInt())
                ">>" -> PyInt(left.value shr right.value.toInt())
                else -> throw PyRuntimeError("TypeError", "unsupported operand type(s) for $op: 'int' and 'int'", line)
            }
        }

        // Float / Int arithmetic
        if ((left is PyFloat && (right is PyFloat || right is PyInt)) || (left is PyInt && right is PyFloat)) {
            val l = if (left is PyFloat) left.value else (left as PyInt).value.toDouble()
            val r = if (right is PyFloat) right.value else (right as PyInt).value.toDouble()
            return when (op) {
                "+" -> PyFloat(l + r)
                "-" -> PyFloat(l - r)
                "*" -> PyFloat(l * r)
                "/" -> {
                    if (r == 0.0) throw PyRuntimeError("ZeroDivisionError", "float division by zero", line)
                    PyFloat(l / r)
                }
                "//" -> {
                    if (r == 0.0) throw PyRuntimeError("ZeroDivisionError", "float floor division by zero", line)
                    PyFloat(kotlin.math.floor(l / r))
                }
                "%" -> {
                    if (r == 0.0) throw PyRuntimeError("ZeroDivisionError", "float modulo", line)
                    PyFloat(l % r)
                }
                "**" -> PyFloat(l.pow(r))
                else -> throw PyRuntimeError("TypeError", "unsupported operand type(s) for $op: '${left.typeName()}' and '${right.typeName()}'", line)
            }
        }

        // String operations
        if (left is PyStr) {
            if (op == "+" && right is PyStr) return PyStr(left.value + right.value)
            if (op == "*" && right is PyInt) return PyStr(left.value.repeat(right.value.coerceAtLeast(0L).toInt()))
        }
        if (left is PyInt && right is PyStr && op == "*") {
            return PyStr(right.value.repeat(left.value.coerceAtLeast(0L).toInt()))
        }

        // List operations
        if (left is PyList) {
            if (op == "+" && right is PyList) {
                val combined = (left.elements + right.elements).toMutableList()
                return PyList(combined)
            }
            if (op == "*" && right is PyInt) {
                val repeated = mutableListOf<PyValue>()
                val count = right.value.coerceAtLeast(0L).toInt()
                for (i in 0 until count) repeated.addAll(left.elements)
                return PyList(repeated)
            }
        }

        // Tuple operations
        if (left is PyTuple) {
            if (op == "+" && right is PyTuple) {
                return PyTuple(left.elements + right.elements)
            }
            if (op == "*" && right is PyInt) {
                val repeated = mutableListOf<PyValue>()
                val count = right.value.coerceAtLeast(0L).toInt()
                for (i in 0 until count) repeated.addAll(left.elements)
                return PyTuple(repeated)
            }
        }

        // Set bitwise ops
        if (left is PySet && right is PySet) {
            return when (op) {
                "|" -> PySet(LinkedHashSet(left.set).apply { addAll(right.set) })
                "&" -> PySet(LinkedHashSet(left.set).apply { retainAll(right.set) })
                "-" -> PySet(LinkedHashSet(left.set).apply { removeAll(right.set) })
                "^" -> {
                    val sym = LinkedHashSet(left.set)
                    for (item in right.set) {
                        if (sym.contains(item)) sym.remove(item) else sym.add(item)
                    }
                    PySet(sym)
                }
                else -> throw PyRuntimeError("TypeError", "unsupported operand type(s) for $op: 'set' and 'set'", line)
            }
        }

        throw PyRuntimeError("TypeError", "unsupported operand type(s) for $op: '${left.typeName()}' and '${right.typeName()}'", line)
    }

    private fun formatString(format: String, argsVal: PyValue): PyStr {
        val args = if (argsVal is PyTuple) argsVal.elements else listOf(argsVal)
        val sb = StringBuilder()
        var argIdx = 0
        var i = 0
        while (i < format.length) {
            if (format[i] == '%' && i + 1 < format.length) {
                val spec = format[i + 1]
                if (spec == '%') {
                    sb.append('%')
                    i += 2
                    continue
                }
                val arg = args.getOrNull(argIdx++) ?: throw PyRuntimeError("TypeError", "not enough arguments for format string")
                when (spec) {
                    's' -> sb.append(arg.str())
                    'd', 'i' -> sb.append((arg as? PyInt)?.value?.toString() ?: arg.str())
                    'f' -> sb.append((arg as? PyFloat)?.value?.toString() ?: arg.str())
                    'r' -> sb.append(arg.repr())
                    else -> sb.append("%$spec")
                }
                i += 2
            } else {
                sb.append(format[i++])
            }
        }
        return PyStr(sb.toString())
    }

    private fun applyComparison(left: PyValue, op: String, right: PyValue, line: Int): Boolean {
        return when (op) {
            "==" -> left == right
            "!=" -> left != right
            "<" -> left < right
            "<=" -> left <= right
            ">" -> left > right
            ">=" -> left >= right
            "is" -> left === right
            "is not" -> left !== right
            "in" -> {
                when (right) {
                    is PyList -> right.elements.contains(left)
                    is PyTuple -> right.elements.contains(left)
                    is PySet -> right.set.contains(left)
                    is PyDict -> right.map.containsKey(left)
                    is PyStr -> {
                        if (left !is PyStr) throw PyRuntimeError("TypeError", "'in <string>' requires string as left operand, not ${left.typeName()}", line)
                        right.value.contains(left.value)
                    }
                    is PyRange -> {
                        if (left is PyInt) right.toList().contains(left) else false
                    }
                    else -> throw PyRuntimeError("TypeError", "argument of type '${right.typeName()}' is not iterable", line)
                }
            }
            "not in" -> !applyComparison(left, "in", right, line)
            else -> false
        }
    }
}
