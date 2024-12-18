package com.example.notesapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.scribblespace.Note

class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "notes.db"
        private const val TABLE_NOTES = "notes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_PASSWORD = "password" // Новый столбец для пароля
    }
    fun deleteNote(id: Long) {
        val db = this.writableDatabase
        db.delete("notes", "id=?", arrayOf(id.toString()))
        db.close()
    }
    fun updateNote(note: Note): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("title", note.title)
            put("content", note.content)
            put("password", note.password)
        }
        // Обновляем заметку в базе данных по ID
        return db.update(TABLE_NOTES, values, "id = ?", arrayOf(note.id.toString()))
    }
    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = ("CREATE TABLE $TABLE_NOTES (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TITLE TEXT," +
                "$COLUMN_CONTENT TEXT," +
                "$COLUMN_PASSWORD TEXT)") // Добавляем столбец для пароля
        db?.execSQL(createTable)
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            // Добавляем новый столбец для пароля, если он отсутствует
            val addPasswordColumn = "ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_PASSWORD TEXT"
            db?.execSQL(addPasswordColumn)
        }
    }
    // Метод для добавления заметки
    fun addNote(note: Note): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_PASSWORD, note.password) // Добавляем пароль (если он есть)
        }
        val id = db.insert(TABLE_NOTES, null, values)
        db.close()
        return id
    }
    // Метод для получения всех заметок
    fun getAllNotes(): List<Note> {
        val notesList = mutableListOf<Note>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NOTES", null)

        if (cursor.moveToFirst()) {
            do {
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
                val contentIndex = cursor.getColumnIndex(COLUMN_CONTENT)
                val passwordIndex = cursor.getColumnIndex(COLUMN_PASSWORD) // Читаем пароль из базы данных

                if (idIndex != -1 && titleIndex != -1 && contentIndex != -1) {
                    val note = Note(
                        id = cursor.getLong(idIndex),
                        title = cursor.getString(titleIndex),
                        content = cursor.getString(contentIndex),
                        password = cursor.getString(passwordIndex) // Сохраняем пароль, если он есть
                    )
                    notesList.add(note)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return notesList
    }
}

