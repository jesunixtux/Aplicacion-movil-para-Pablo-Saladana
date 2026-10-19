package cl.duoc.coegio_pablo_saladana.data

import android.content.Context

class NotebookRepository(context: Context) {
    private val notebookDao = AppDatabase.getInstance(context).notebookDao()

    suspend fun findAll(): List<NotebookEntity> = notebookDao.findAll()

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null

        val normalizedDraft = draft.normalized()
        if (!normalizedDraft.isValid()) return null

        val notebook = NotebookEntity(
            title = normalizedDraft.title,
            purpose = normalizedDraft.purpose,
            instructions = normalizedDraft.instructions,
            questions = normalizedDraft.questions,
            teacherEmail = teacher.email,
            teacherName = teacher.fullName,
            createdAt = System.currentTimeMillis()
        )
        val id = notebookDao.insert(notebook)
        return notebook.copy(id = id)
    }
}
