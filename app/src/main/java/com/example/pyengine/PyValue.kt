package com.example.pyengine

class PyRuntimeError(
    val typeName: String,
    override val message: String,
    val line: Int = -1,
    val traceback: MutableList<String> = mutableListOf()
) : Exception("$typeName: $message")

sealed class PyValue : Comparable<PyValue> {
    abstract fun typeName(): String
    abstract fun str(): String
    abstract fun repr(): String
    abstract fun isTruthy(): Boolean

    override fun toString(): String = str()

    override fun compareTo(other: PyValue): Int {
        if (this is PyInt && other is PyInt) return value.compareTo(other.value)
        if (this is PyFloat && other is PyFloat) return value.compareTo(other.value)
        if (this is PyInt && other is PyFloat) return value.toDouble().compareTo(other.value)
        if (this is PyFloat && other is PyInt) return value.compareTo(other.value.toDouble())
        if (this is PyStr && other is PyStr) return value.compareTo(other.value)
        if (this is PyBool && other is PyBool) return value.compareTo(other.value)
        throw PyRuntimeError("TypeError", "'<' not supported between instances of '${typeName()}' and '${other.typeName()}'")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PyValue) return false
        if (this is PyInt && other is PyInt) return value == other.value
        if (this is PyFloat && other is PyFloat) return value == other.value
        if (this is PyInt && other is PyFloat) return value.toDouble() == other.value
        if (this is PyFloat && other is PyInt) return value == other.value.toDouble()
        if (this is PyStr && other is PyStr) return value == other.value
        if (this is PyBool && other is PyBool) return value == other.value
        if (this is PyNone && other is PyNone) return true
        if (this is PyList && other is PyList) return elements == other.elements
        if (this is PyTuple && other is PyTuple) return elements == other.elements
        if (this is PyDict && other is PyDict) return map == other.map
        if (this is PySet && other is PySet) return set == other.set
        return this === other
    }

    override fun hashCode(): Int {
        return when (this) {
            is PyInt -> value.hashCode()
            is PyFloat -> value.hashCode()
            is PyStr -> value.hashCode()
            is PyBool -> value.hashCode()
            is PyNone -> 0
            is PyTuple -> elements.hashCode()
            is PySet -> set.hashCode()
            else -> System.identityHashCode(this)
        }
    }
}

data class PyInt(val value: Long) : PyValue() {
    override fun typeName(): String = "int"
    override fun str(): String = value.toString()
    override fun repr(): String = value.toString()
    override fun isTruthy(): Boolean = value != 0L
}

data class PyFloat(val value: Double) : PyValue() {
    override fun typeName(): String = "float"
    override fun str(): String {
        val s = value.toString()
        return if (!s.contains('.') && !s.contains('e') && !s.contains('E')) "$s.0" else s
    }
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = value != 0.0 && !value.isNaN()
}

data class PyStr(val value: String) : PyValue() {
    override fun typeName(): String = "str"
    override fun str(): String = value
    override fun repr(): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\r", "\\r")
        return "'$escaped'"
    }
    override fun isTruthy(): Boolean = value.isNotEmpty()
}

data class PyBool(val value: Boolean) : PyValue() {
    override fun typeName(): String = "bool"
    override fun str(): String = if (value) "True" else "False"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = value
}

object PyNone : PyValue() {
    override fun typeName(): String = "NoneType"
    override fun str(): String = "None"
    override fun repr(): String = "None"
    override fun isTruthy(): Boolean = false
}

data class PyList(val elements: MutableList<PyValue> = mutableListOf()) : PyValue() {
    override fun typeName(): String = "list"
    override fun str(): String = "[${elements.joinToString(", ") { it.repr() }}]"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = elements.isNotEmpty()
}

data class PyTuple(val elements: List<PyValue> = emptyList()) : PyValue() {
    override fun typeName(): String = "tuple"
    override fun str(): String {
        return if (elements.size == 1) "(${elements[0].repr()},)"
        else "(${elements.joinToString(", ") { it.repr() }})"
    }
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = elements.isNotEmpty()
}

data class PyDict(val map: LinkedHashMap<PyValue, PyValue> = LinkedHashMap()) : PyValue() {
    override fun typeName(): String = "dict"
    override fun str(): String {
        val items = map.entries.joinToString(", ") { "${it.key.repr()}: ${it.value.repr()}" }
        return "{$items}"
    }
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = map.isNotEmpty()
}

data class PySet(val set: LinkedHashSet<PyValue> = LinkedHashSet()) : PyValue() {
    override fun typeName(): String = "set"
    override fun str(): String {
        if (set.isEmpty()) return "set()"
        return "{${set.joinToString(", ") { it.repr() }}}"
    }
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = set.isNotEmpty()
}

data class PyRange(val start: Long, val stop: Long, val step: Long = 1L) : PyValue() {
    override fun typeName(): String = "range"
    override fun str(): String = if (step == 1L) "range($start, $stop)" else "range($start, $stop, $step)"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = length() > 0

    fun length(): Long {
        if (step > 0 && start < stop) return 1 + (stop - 1 - start) / step
        if (step < 0 && start > stop) return 1 + (start - 1 - stop) / (-step)
        return 0L
    }

    fun toList(): List<PyInt> {
        val result = mutableListOf<PyInt>()
        var curr = start
        if (step > 0) {
            while (curr < stop) {
                result.add(PyInt(curr))
                curr += step
            }
        } else if (step < 0) {
            while (curr > stop) {
                result.add(PyInt(curr))
                curr += step
            }
        }
        return result
    }
}

data class PyFunction(
    val name: String,
    val params: List<String>,
    val defaults: Map<String, PyValue>,
    val vararg: String?,
    val kwarg: String?,
    val body: List<Stmt>,
    val closureScope: PyScope
) : PyValue() {
    override fun typeName(): String = "function"
    override fun str(): String = "<function $name>"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = true
}

data class PyBuiltinFunction(
    val name: String,
    val func: suspend (args: List<PyValue>, kwargs: Map<String, PyValue>, context: PyExecutionContext) -> PyValue
) : PyValue() {
    override fun typeName(): String = "builtin_function_or_method"
    override fun str(): String = "<built-in function $name>"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = true
}

data class PyClass(
    val name: String,
    val bases: List<PyClass>,
    val members: MutableMap<String, PyValue> = mutableMapOf()
) : PyValue() {
    override fun typeName(): String = "type"
    override fun str(): String = "<class '$name'>"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = true

    fun findMember(attr: String): PyValue? {
        members[attr]?.let { return it }
        for (base in bases) {
            base.findMember(attr)?.let { return it }
        }
        return null
    }
}

data class PyInstance(
    val pyClass: PyClass,
    val fields: MutableMap<String, PyValue> = mutableMapOf()
) : PyValue() {
    override fun typeName(): String = pyClass.name
    override fun str(): String {
        val strMethod = getAttr("__str__")
        if (strMethod is PyBoundMethod) {
            // Evaluated during runtime if available
        }
        return "<${pyClass.name} object at 0x${System.identityHashCode(this).toString(16)}>"
    }
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = true

    fun getAttr(name: String): PyValue? {
        fields[name]?.let { return it }
        val member = pyClass.findMember(name)
        if (member is PyFunction) {
            return PyBoundMethod(this, member)
        }
        return member
    }

    fun setAttr(name: String, value: PyValue) {
        fields[name] = value
    }
}

data class PyBoundMethod(
    val instance: PyInstance,
    val function: PyFunction
) : PyValue() {
    override fun typeName(): String = "method"
    override fun str(): String = "<bound method ${instance.pyClass.name}.${function.name} of ${instance.str()}>"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = true
}

data class PyModule(
    val name: String,
    val members: MutableMap<String, PyValue> = mutableMapOf()
) : PyValue() {
    override fun typeName(): String = "module"
    override fun str(): String = "<module '$name'>"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = true
}

data class PyFile(
    val filename: String,
    val mode: String,
    val content: StringBuilder,
    var position: Int = 0,
    var isOpen: Boolean = true,
    val onSave: ((String, String) -> Unit)? = null
) : PyValue() {
    override fun typeName(): String = "_io.TextIOWrapper"
    override fun str(): String = "<_io.TextIOWrapper name='$filename' mode='$mode' encoding='UTF-8'>"
    override fun repr(): String = str()
    override fun isTruthy(): Boolean = isOpen
}
