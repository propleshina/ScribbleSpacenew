package com.example.scribblespace

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.DatabaseHandler
import android.widget.Toast

data class Note(
    var id: Long = -1,
    var title: String,
    var content: String,
    val password: String? = null
)

class NotesAdapter(
    context: Context,
    private val notes: MutableList<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteDelete: (Note) -> Unit
) : ArrayAdapter<Note>(context, 0, notes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val note = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false)
        val titleTextView = view.findViewById<TextView>(android.R.id.text1)
        val contentTextView = view.findViewById<TextView>(android.R.id.text2)

        titleTextView.text = note?.title

        // Измененное условие для проверки наличия пароля
        contentTextView.text = if (note?.password.isNullOrEmpty()) {
            note?.content // Если пароль пустой или отсутствует, показываем содержимое
        } else {
            "****" // Если установлен пароль, показываем "****"
        }

        view.setOnClickListener {
            note?.let { onNoteClick(it) }
        }

        view.setOnLongClickListener {
            note?.let { onNoteDelete(it) }
            true
        }
        return view
    }

}

class MainActivity : AppCompatActivity() {
    private lateinit var notesListView: ListView
    private lateinit var notesAdapter: NotesAdapter
    private val notes = mutableListOf<Note>()
    private lateinit var databaseHandler: DatabaseHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHandler = DatabaseHandler(this)
        notesListView = findViewById(R.id.listView)

        // Устанавливаем адаптер
        notesAdapter = NotesAdapter(this, notes, onNoteClick = { note -> editNote(note) }, onNoteDelete = { note -> deleteNote(note) })
        notesListView.adapter = notesAdapter

        loadNotes()

        findViewById<Button>(R.id.addNoteButton).setOnClickListener { addNewNote() }
    }

    private fun loadNotes() {
        notes.clear()
        notes.addAll(databaseHandler.getAllNotes())
        notesAdapter.notifyDataSetChanged()
    }

    private fun addNewNote() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_add_note, null)
        dialogBuilder.setView(dialogView)

        val titleEditText = dialogView.findViewById<EditText>(R.id.noteTitleEditText)
        val contentEditText = dialogView.findViewById<EditText>(R.id.noteContentEditText)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.notePasswordEditText)

        dialogBuilder.setTitle("Добавить новую заметку")
        dialogBuilder.setPositiveButton("Добавить") { dialog, _ ->
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val password = passwordEditText.text.toString()

            val newNote = Note(title = title, content = content, password = password)
            val noteId = databaseHandler.addNote(newNote)
            newNote.id = noteId
            notes.add(newNote)

            notesAdapter.notifyDataSetChanged()
        }
        dialogBuilder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }

    private fun editNote(note: Note) {
        // Если заметка защищена паролем, сначала проверим пароль
        if (note.password!!.isNotEmpty()) {
            val passwordInput = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            AlertDialog.Builder(this)
                .setTitle("Введите пароль")
                .setView(passwordInput)
                .setPositiveButton("OK") { dialog, _ ->
                    if (passwordInput.text.toString() == note.password) {
                        // Открываем окно для редактирования заметки
                        openEditNoteDialog(note)
                    } else {
                        // Если пароль неверный, показываем сообщение
                        Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            // Если пароль не установлен, сразу открываем диалог редактирования
            openEditNoteDialog(note)
        }
    }
    private fun openEditNoteDialog(note: Note) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_note, null)
        dialogBuilder.setView(dialogView)

        // Инициализируем EditText для редактирования
        val titleEditText = dialogView.findViewById<EditText>(R.id.titleEditText)
        val contentEditText = dialogView.findViewById<EditText>(R.id.contentEditText)

        // Заполняем поля данными из заметки
        titleEditText.setText(note.title)
        contentEditText.setText(note.content)

        dialogBuilder.setTitle("Редактировать заметку")
        dialogBuilder.setPositiveButton("Сохранить") { dialog, _ ->
            // Сохраняем изменения в базе данных
            val updatedTitle = titleEditText.text.toString()
            val updatedContent = contentEditText.text.toString()

            note.title = updatedTitle
            note.content = updatedContent

            // Обновляем заметку в базе данных
            databaseHandler.updateNote(note)

            // Обновляем список заметок
            loadNotes()
        }

        dialogBuilder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }

    private fun deleteNote(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setMessage("Вы уверены, что хотите удалить эту заметку?")
            .setPositiveButton("Да") { _, _ ->
                databaseHandler.deleteNote(note.id)
                loadNotes()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}

