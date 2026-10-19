package cl.duoc.coegio_pablo_saladana.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

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

    fun hasValidClassDate(): Boolean {
        if (!CLASS_DATE_PATTERN.matches(classDate)) return false
        val position = ParsePosition(0)
        val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }.parse(classDate, position)
        return parsedDate != null && position.index == classDate.length
    }

    fun isComplete(): Boolean = hasValidClassDate() &&
        content.isNotBlank() && activity.isNotBlank() && learning.isNotBlank()

    private companion object {
        val CLASS_DATE_PATTERN = Regex("\\d{2}/\\d{2}/\\d{4}")
    }
}
