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
    suspend fun fetchByStudent(email: String): List<StudentEntryPayload>? = runCatching {
        SchoolBackend.api.entries(studentEmail = email)
    }.getOrNull()

    suspend fun fetchByTeacher(email: String): List<StudentEntryPayload>? = runCatching {
        SchoolBackend.api.entries(teacherEmail = email)
    }.getOrNull()

    suspend fun create(entry: StudentEntryEntity, notebook: NotebookEntity): StudentEntryPayload? {
        val remoteNotebookId = notebook.remoteId ?: return null
        return runCatching {
            SchoolBackend.api.createEntry(input(entry, remoteNotebookId))
        }.getOrNull()
    }

    suspend fun update(entry: StudentEntryEntity, notebook: NotebookEntity): StudentEntryPayload? {
        val remoteId = entry.remoteId ?: return create(entry, notebook)
        val remoteNotebookId = notebook.remoteId ?: return null
        return runCatching {
            SchoolBackend.api.updateEntry(remoteId, input(entry, remoteNotebookId))
        }.getOrNull()
    }

    private fun input(entry: StudentEntryEntity, notebookId: Long) = StudentEntryInput(
        notebookId, entry.notebookTitle, entry.studentEmail, entry.studentName,
        entry.classDate, entry.content, entry.activity, entry.learning, entry.doubts,
        entry.isDraft, entry.isReviewed, entry.teacherFeedback
    )
}
