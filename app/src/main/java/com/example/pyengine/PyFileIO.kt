package com.example.pyengine

import java.io.File

class PyFileIO(private val baseDir: File) {

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    fun exists(filename: String): Boolean {
        val file = resolveFile(filename)
        return file.exists()
    }

    fun listFiles(): List<String> {
        return baseDir.list()?.toList() ?: emptyList()
    }

    fun readFile(filename: String): String {
        val file = resolveFile(filename)
        if (!file.exists()) {
            throw PyRuntimeError("FileNotFoundError", "No such file or directory: '$filename'")
        }
        return file.readText()
    }

    fun writeFile(filename: String, content: String) {
        val file = resolveFile(filename)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun appendFile(filename: String, content: String) {
        val file = resolveFile(filename)
        file.parentFile?.mkdirs()
        file.appendText(content)
    }

    fun deleteFile(filename: String): Boolean {
        val file = resolveFile(filename)
        return if (file.exists()) file.delete() else false
    }

    fun open(filename: String, mode: String = "r"): PyFile {
        val initialContent = StringBuilder()
        if (mode.contains('r') || mode.contains('a') || mode.contains('+')) {
            val file = resolveFile(filename)
            if (file.exists()) {
                initialContent.append(file.readText())
            } else if (mode == "r") {
                throw PyRuntimeError("FileNotFoundError", "[Errno 2] No such file or directory: '$filename'")
            }
        }

        return PyFile(
            filename = filename,
            mode = mode,
            content = initialContent,
            position = if (mode.contains('a')) initialContent.length else 0,
            isOpen = true,
            onSave = { name, data ->
                if (mode.contains('w')) {
                    writeFile(name, data)
                } else if (mode.contains('a')) {
                    appendFile(name, data)
                } else if (mode.contains('+')) {
                    writeFile(name, data)
                }
            }
        )
    }

    private fun resolveFile(filename: String): File {
        return if (filename.startsWith("/")) {
            val rel = filename.trimStart('/')
            File(baseDir, rel)
        } else {
            File(baseDir, filename)
        }
    }
}
