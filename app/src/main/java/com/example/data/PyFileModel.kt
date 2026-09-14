package com.example.data

data class PyFileModel(
    val name: String,
    val path: String,
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val isModified: Boolean = false
)
