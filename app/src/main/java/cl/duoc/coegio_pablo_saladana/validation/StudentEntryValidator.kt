package cl.duoc.coegio_pablo_saladana.validation

import cl.duoc.coegio_pablo_saladana.data.StudentEntryDraft
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

/** Verifica borradores y registros de clase antes de guardarlos. */
object StudentEntryValidator {
    private val classDatePattern = Regex("\\d{2}/\\d{2}/\\d{4}")

    data class Errors(
        val classDate: Boolean,
        val content: Boolean,
        val activity: Boolean,
        val learning: Boolean
    ) {
        val isValid: Boolean get() = !classDate && !content && !activity && !learning
    }

    fun hasContent(draft: StudentEntryDraft): Boolean = draft.content.isNotBlank() ||
        draft.activity.isNotBlank() || draft.learning.isNotBlank() || draft.doubts.isNotBlank()

    /** Exige una fecha real con el formato que muestra el formulario: dd/MM/aaaa. */
    fun hasValidClassDate(draft: StudentEntryDraft): Boolean {
        if (!classDatePattern.matches(draft.classDate)) return false
        val position = ParsePosition(0)
        val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }.parse(draft.classDate, position)
        return parsedDate != null && position.index == draft.classDate.length
    }

    fun validateSubmission(draft: StudentEntryDraft): Errors = Errors(
        classDate = !hasValidClassDate(draft),
        content = draft.content.isBlank(),
        activity = draft.activity.isBlank(),
        learning = draft.learning.isBlank()
    )

    fun isComplete(draft: StudentEntryDraft): Boolean = validateSubmission(draft).isValid
}
