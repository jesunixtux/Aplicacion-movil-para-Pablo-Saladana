package cl.duoc.coegio_pablo_saladana.data

import android.content.Context

/** Gestiona los registros de clase y evita que cuentas docentes los creen como estudiantes. */
class StudentEntryRepository(context: Context) {
    private val entryDao = AppDatabase.getInstance(context).studentEntryDao()

    suspend fun findByStudent(email: String): List<StudentEntryEntity> =
        entryDao.findByStudent(email)

    suspend fun create(
        draft: StudentEntryDraft,
        isDraft: Boolean,
        student: UserAccount,
        notebook: NotebookEntity
    ): StudentEntryEntity? {
        if (student.role != UserRole.Student) return null

        val normalizedDraft = draft.normalized()
        if (!normalizedDraft.hasContent() || (!isDraft && !normalizedDraft.isComplete())) return null

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
}
