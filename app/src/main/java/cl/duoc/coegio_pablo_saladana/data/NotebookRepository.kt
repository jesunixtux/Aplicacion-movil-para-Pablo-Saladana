package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import cl.duoc.coegio_pablo_saladana.validation.NotebookValidator

/** Gestiona la consulta y publicación de bitácoras en Room y en el microservicio Spring Boot. */
class NotebookRepository(context: Context) {
    private val notebookDao = AppDatabase.getInstance(context).notebookDao()

    suspend fun findAll(): List<NotebookEntity> = notebookDao.findAll()

    /** Sincroniza bitácoras desde el microservicio Spring Boot sin duplicar registros locales. */
    suspend fun syncWithBackend(): List<NotebookEntity> {
        val remoteNotebooks = NotebookRemoteDataSource.fetchAll()
        if (remoteNotebooks != null) {
            for (remote in remoteNotebooks) {
                if (notebookDao.findByTitleAndTeacher(remote.title, remote.teacherEmail) == null) {
                    notebookDao.insert(remote.copy(id = 0))
                }
            }
        }
        return notebookDao.findAll()
    }

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null

        val normalizedDraft = draft.normalized()
        if (!NotebookValidator.isValid(normalizedDraft)) return null

        val remoteCreated = NotebookRemoteDataSource.create(normalizedDraft, teacher)
        val notebook = NotebookEntity(
            title = normalizedDraft.title,
            purpose = normalizedDraft.purpose,
            instructions = normalizedDraft.instructions,
            questions = normalizedDraft.questions,
            teacherEmail = teacher.email,
            teacherName = teacher.fullName,
            createdAt = remoteCreated?.createdAt ?: System.currentTimeMillis()
        )
        val id = notebookDao.insert(notebook)
        return notebook.copy(id = id)
    }
}
