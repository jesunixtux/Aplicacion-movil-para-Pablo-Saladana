package cl.duoc.coegio_pablo_saladana.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Registro de una clase escrito por un estudiante para una bitácora publicada. */
@Entity(tableName = "student_entries")
data class StudentEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long,
    val notebookTitle: String,
    val studentEmail: String,
    val studentName: String,
    val classDate: String,
    val content: String,
    val activity: String,
    val learning: String,
    val doubts: String,
    val isDraft: Boolean,
    val createdAt: Long
)

/** Información editable de un registro antes de guardarlo como borrador o enviarlo. */
data class StudentEntryDraft(
    val classDate: String,
    val content: String,
    val activity: String,
    val learning: String,
    val doubts: String
) {
    fun normalized(): StudentEntryDraft = copy(
        classDate = classDate.trim(),
        content = content.trim(),
        activity = activity.trim(),
        learning = learning.trim(),
        doubts = doubts.trim()
    )
}
