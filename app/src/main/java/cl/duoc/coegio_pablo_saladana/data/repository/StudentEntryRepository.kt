package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.StudentEntryPayload
import cl.duoc.coegio_pablo_saladana.data.remote.StudentEntryRemoteDataSource
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.StudentEntryValidator

/** Gestiona los registros de clase y evita que cuentas docentes los creen como estudiantes. */
class StudentEntryRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val entryDao = database.studentEntryDao()
    private val notebookDao = database.notebookDao()

    suspend fun findLocalByStudent(email: String): List<StudentEntryEntity> = entryDao.findByStudent(email)

    suspend fun findLocalSubmittedByTeacher(email: String): List<StudentEntryEntity> =
        entryDao.findSubmittedByTeacher(email)

    suspend fun findByStudent(email: String): List<StudentEntryEntity> {
        sync(StudentEntryRemoteDataSource.fetchByStudent(email))
        return entryDao.findByStudent(email)
    }

    suspend fun findSubmittedByTeacher(email: String): List<StudentEntryEntity> {
        sync(StudentEntryRemoteDataSource.fetchByTeacher(email))
        return entryDao.findSubmittedByTeacher(email)
    }

    private suspend fun sync(remoteEntries: List<StudentEntryPayload>?) {
        for (remote in remoteEntries ?: return) {
            val notebook = notebookDao.findByRemoteId(remote.notebookId) ?: continue
            val existing = entryDao.findByRemoteId(remote.id)
            val entity = remote.toEntity(notebook)
            if (existing == null) entryDao.insert(entity)
            else entryDao.update(entity.copy(id = existing.id))
        }
    }

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
        val remote = StudentEntryRemoteDataSource.create(entry, notebook)
            ?.takeIf { it.id > 0 } ?: return null
        val saved = remote.toEntity(notebook)
        val id = entryDao.insert(saved)
        return saved.copy(id = id)
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

        val existing = entryDao.findById(entryId)
            ?.takeIf { it.isDraft && it.studentEmail.equals(student.email, ignoreCase = true) }
            ?: return null
        val notebook = notebookDao.findById(existing.notebookId) ?: return null
        val candidate = existing.copy(
            classDate = normalizedDraft.classDate,
            content = normalizedDraft.content,
            activity = normalizedDraft.activity,
            learning = normalizedDraft.learning,
            doubts = normalizedDraft.doubts,
            isDraft = isDraft
        )
        val remote = StudentEntryRemoteDataSource.update(candidate, notebook) ?: return null
        val updated = remote.toEntity(notebook).copy(id = existing.id)
        entryDao.update(updated)
        return updated
    }

    suspend fun markReviewed(
        entryId: Long,
        teacher: UserAccount,
        feedback: String
    ): StudentEntryEntity? {
        if (teacher.role != UserRole.Teacher) return null
        val existing = entryDao.findById(entryId)?.takeIf { !it.isDraft } ?: return null
        val notebook = notebookDao.findById(existing.notebookId)
            ?.takeIf { it.teacherEmail.equals(teacher.email, ignoreCase = true) } ?: return null
        val remote = StudentEntryRemoteDataSource.update(
            existing.copy(isReviewed = true, teacherFeedback = feedback.trim()), notebook
        ) ?: return null
        val updated = remote.toEntity(notebook).copy(id = existing.id)
        entryDao.update(updated)
        return updated
    }
}
