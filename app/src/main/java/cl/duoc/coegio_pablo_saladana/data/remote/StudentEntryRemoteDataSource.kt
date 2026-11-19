package cl.duoc.coegio_pablo_saladana.data.remote

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity

internal data class StudentEntryPayload(
    val id: Long = 0,
    val notebookId: Long = 0,
    val notebookTitle: String? = null,
    val studentEmail: String? = null,
    val studentName: String? = null,
    val classDate: String? = null,
    val content: String? = null,
    val activity: String? = null,
    val learning: String? = null,
    val doubts: String? = null,
    val isDraft: Boolean = false,
    val createdAt: Long = 0,
    val isReviewed: Boolean = false,
    val teacherFeedback: String? = null
) {
    fun toEntity(notebook: NotebookEntity): StudentEntryEntity = StudentEntryEntity(
        notebookId = notebook.id,
        notebookTitle = notebookTitle ?: notebook.title,
        studentEmail = studentEmail.orEmpty(),
        studentName = studentName.orEmpty(),
        classDate = classDate.orEmpty(),
        content = content.orEmpty(),
        activity = activity.orEmpty(),
        learning = learning.orEmpty(),
        doubts = doubts.orEmpty(),
        isDraft = isDraft,
        createdAt = createdAt,
        isReviewed = isReviewed,
        teacherFeedback = teacherFeedback.orEmpty(),
        remoteId = id.takeIf { it > 0 }
    )
}

internal data class StudentEntryInput(
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
    val isReviewed: Boolean,
    val teacherFeedback: String
)

internal object StudentEntryRemoteDataSource {
    /** Motivo del último fallo con el backend; null si la última llamada fue exitosa. */
    var lastError: String? = null
        private set

    suspend fun fetchByStudent(email: String): List<StudentEntryPayload>? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        return runCatching {
            SchoolBackend.api.entries(studentEmail = email)
        }.onFailure { lastError = backendMessage(it) }.getOrNull()
    }

    suspend fun fetchByTeacher(email: String): List<StudentEntryPayload>? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        return runCatching {
            SchoolBackend.api.entries(teacherEmail = email)
        }.onFailure { lastError = backendMessage(it) }.getOrNull()
    }

    suspend fun create(entry: StudentEntryEntity, notebook: NotebookEntity): StudentEntryPayload? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        val remoteNotebookId = notebook.remoteId ?: run {
            lastError = "La bitácora no existe en el servidor; no se puede enviar el registro."
            return null
        }
        return runCatching {
            SchoolBackend.api.createEntry(input(entry, remoteNotebookId))
        }.onFailure { lastError = backendMessage(it) }.getOrNull()
    }

    suspend fun update(entry: StudentEntryEntity, notebook: NotebookEntity): StudentEntryPayload? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        val remoteId = entry.remoteId ?: return create(entry, notebook)
        val remoteNotebookId = notebook.remoteId ?: run {
            lastError = "La bitácora no existe en el servidor; no se puede actualizar el registro."
            return null
        }
        return runCatching {
            SchoolBackend.api.updateEntry(remoteId, input(entry, remoteNotebookId))
        }.onFailure { lastError = backendMessage(it) }.getOrNull()
    }

    private fun backendMessage(error: Throwable): String =
        "Error del backend de registros (${cl.duoc.coegio_pablo_saladana.BuildConfig.API_BASE_URL}). " +
            cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.describe(error)

    private fun input(entry: StudentEntryEntity, notebookId: Long) = StudentEntryInput(
        notebookId, entry.notebookTitle, entry.studentEmail, entry.studentName,
        entry.classDate, entry.content, entry.activity, entry.learning, entry.doubts,
        entry.isDraft, entry.isReviewed, entry.teacherFeedback
    )
}
