package com.example.data

import android.content.Context
import java.io.File

class PyFileManager(context: Context) {
    val scriptsDir: File = File(context.filesDir, "python_scripts")

    init {
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
        }
        initializeDefaultFiles()
    }

    private fun initializeDefaultFiles() {
        val existing = listFiles()
        if (existing.isEmpty()) {
            for ((filename, content) in DefaultTemplates.ALL_TEMPLATES) {
                val file = File(scriptsDir, filename)
                if (!file.exists()) {
                    file.writeText(content)
                }
            }
        }
    }

    fun listFiles(): List<PyFileModel> {
        val files = scriptsDir.listFiles { file -> file.isFile && file.name.endsWith(".py") } ?: return emptyList()
        return files.sortedBy { it.name.lowercase() }.map { f ->
            PyFileModel(
                name = f.name,
                path = f.absolutePath,
                sizeBytes = f.length(),
                lastModified = f.lastModified()
            )
        }
    }

    fun readFile(name: String): String {
        val sanitized = sanitizeFilename(name)
        val file = File(scriptsDir, sanitized)
        return if (file.exists()) file.readText() else ""
    }

    fun saveFile(name: String, content: String): Boolean {
        return try {
            val sanitized = sanitizeFilename(name)
            val file = File(scriptsDir, sanitized)
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createFile(name: String, initialContent: String = ""): Boolean {
        val sanitized = sanitizeFilename(name)
        val file = File(scriptsDir, sanitized)
        if (file.exists()) return false
        return try {
            file.writeText(initialContent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun renameFile(oldName: String, newName: String): Boolean {
        val oldSanitized = sanitizeFilename(oldName)
        val newSanitized = sanitizeFilename(newName)
        val oldFile = File(scriptsDir, oldSanitized)
        val newFile = File(scriptsDir, newSanitized)
        if (!oldFile.exists() || newFile.exists()) return false
        return oldFile.renameTo(newFile)
    }

    fun deleteFile(name: String): Boolean {
        val sanitized = sanitizeFilename(name)
        val file = File(scriptsDir, sanitized)
        return if (file.exists()) file.delete() else false
    }

    private fun sanitizeFilename(name: String): String {
        var clean = name.trim()
        if (!clean.endsWith(".py")) {
            clean += ".py"
        }
        return clean
    }
}
