package cl.duoc.coegio_pablo_saladana.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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

    fun hasContent(): Boolean = content.isNotBlank() ||
        activity.isNotBlank() || learning.isNotBlank() || doubts.isNotBlank()

    fun isComplete(): Boolean = classDate.isNotBlank() &&
        content.isNotBlank() && activity.isNotBlank() && learning.isNotBlank()
}
