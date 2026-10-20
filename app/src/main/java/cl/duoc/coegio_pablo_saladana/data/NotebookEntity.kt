package cl.duoc.coegio_pablo_saladana.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Bitácora publicada por un docente y visible para los estudiantes. */
@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val purpose: String,
    val instructions: String,
    val questions: String,
    val teacherEmail: String,
    val teacherName: String,
    val createdAt: Long
)

/** Datos editables antes de publicar una bitácora. */
data class NotebookDraft(
    val title: String,
    val purpose: String,
    val instructions: String,
    val questions: String
) {
    fun normalized(): NotebookDraft = copy(
        title = title.trim(),
        purpose = purpose.trim(),
        instructions = instructions.trim(),
        questions = questions.trim()
    )

    fun isValid(): Boolean = title.isNotBlank() &&
        purpose.isNotBlank() &&
        instructions.isNotBlank()
}
