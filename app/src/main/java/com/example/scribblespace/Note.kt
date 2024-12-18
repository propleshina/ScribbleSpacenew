package com.example.notesapp.com.example.scribblespace

data class Note(
    var id: Long = -1,
    val title: String,
    val content: String,
    val password: String? = null // New field for password (nullable)
)


