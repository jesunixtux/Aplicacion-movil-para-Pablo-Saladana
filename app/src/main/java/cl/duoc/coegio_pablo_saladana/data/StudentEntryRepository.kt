package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import cl.duoc.coegio_pablo_saladana.validation.StudentEntryValidator

/** Gestiona los registros de clase y evita que cuentas docentes los creen como estudiantes. */
class StudentEntryRepository(context: Context) {
    private val entryDao = AppDatabase.getInstance(context).studentEntryDao()

    suspend fun findByStudent(email: String): List<StudentEntryEntity> =
        entryDao.findByStudent(email)

    suspend fun findSubmittedByTeacher(email: String): List<StudentEntryEntity> =
        entryDao.findSubmittedByTeacher(email)

    suspend fun create(
        draft: StudentEntryDraft,
        isDraft: Boolean,
        student: UserAccount,
        notebook: NotebookEntity
    ): StudentEntryEntity? {
        if (student.role != UserRole.Student) return null

        val normalizedDraft = draft.normalized()
        if (!StudentEntryValidator.hasContent(normalizedDraft) ||
            (!isDraft && !StudentEntryValidator.isComplete(normalizedDraft))) return null

        val entry = StudentEntryEntity(
            notebookId = notebook.id,
            notebookTitle = notebook.title,
            studentEmail = student.email,
            studentName = student.fullName,
            classDate = normalizedDraft.classDate,
            content = normalizedDraft.content,
            activity = normalizedDraft.activity,
            learning = normalizedDraft.learning,
            doubts = normalizedDraft.doubts,
            isDraft = isDraft,
            createdAt = System.currentTimeMillis()
        )
        val id = entryDao.insert(entry)
        return entry.copy(id = id)
    }

    suspend fun updateDraft(
        entryId: Long,
        draft: StudentEntryDraft,
        isDraft: Boolean,
        student: UserAccount
    ): StudentEntryEntity? {
        if (student.role != UserRole.Student) return null
        val normalizedDraft = draft.normalized()
        if (!StudentEntryValidator.hasContent(normalizedDraft) ||
            (!isDraft && !StudentEntryValidator.isComplete(normalizedDraft))) return null

        val updated = entryDao.updateDraft(
            id = entryId,
            studentEmail = student.email,
            classDate = normalizedDraft.classDate,
            content = normalizedDraft.content,
            activity = normalizedDraft.activity,
            learning = normalizedDraft.learning,
            doubts = normalizedDraft.doubts,
            isDraft = isDraft
        )
        return if (updated > 0) entryDao.findById(entryId) else null
    }

    suspend fun markReviewed(
        entryId: Long,
        teacher: UserAccount,
        feedback: String
    ): StudentEntryEntity? {
        if (teacher.role != UserRole.Teacher) return null
        val updated = entryDao.markReviewed(entryId, teacher.email, feedback.trim())
        return if (updated > 0) entryDao.findById(entryId) else null
    }
}
