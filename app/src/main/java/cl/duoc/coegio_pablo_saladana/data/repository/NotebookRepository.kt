package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.remote.NotebookRemoteDataSource
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator

/** Gestiona la consulta, publicación y asignación de bitácoras en Room y en el microservicio Spring Boot. */
class NotebookRepository(context: Context) {
    private val notebookDao = AppDatabase.getInstance(context).notebookDao()

    suspend fun findAll(): List<NotebookEntity> = notebookDao.findAll()

    /** Sincroniza bitácoras desde el microservicio Spring Boot sin duplicar registros locales. */
    suspend fun syncWithBackend(): List<NotebookEntity> {
        val remoteNotebooks = NotebookRemoteDataSource.fetchAll()
        if (remoteNotebooks != null) {
            for (remote in remoteNotebooks) {
                val existing = remote.remoteId?.let { notebookDao.findByRemoteId(it) }
                    ?: notebookDao.findByRemoteIdentity(remote.title, remote.teacherEmail, remote.createdAt)
                if (existing == null) notebookDao.insert(remote)
                else notebookDao.update(remote.copy(id = existing.id))
            }
        }
        // shortcut: las bitácoras antiguas creadas solo en Room no se publican automáticamente; migrarlas si se necesita conservarlas en MySQL.
        return notebookDao.findAll()
    }

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null

        val normalizedDraft = draft.normalized()
        if (!NotebookValidator.isValid(normalizedDraft)) return null

        val remoteCreated = NotebookRemoteDataSource.create(normalizedDraft, teacher)
            ?.takeIf { it.remoteId != null } ?: return null
        val id = notebookDao.insert(remoteCreated)
        return remoteCreated.copy(id = id)
    }

    /** Permite al docente asignar o quitar cualquier estudiante y actualizar el curso de la bitácora. */
    suspend fun updateAssignments(
        notebookId: Long,
        teacher: UserAccount,
        course: String,
        studentEmails: Set<String>
    ): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null
        val existing = notebookDao.findById(notebookId)
            ?.takeIf { it.teacherEmail.equals(teacher.email, ignoreCase = true) } ?: return null
        val normalizedCourse = course.trim().ifBlank { SchoolCourses.GENERAL }
        val serializedEmails = studentEmails
            .map { it.trim().lowercase() }
            .filter(String::isNotBlank)
            .sorted()
            .joinToString(",")
        val remoteUpdated = NotebookRemoteDataSource.update(existing.copy(
            course = normalizedCourse,
            assignedStudentEmails = serializedEmails
        )) ?: return null
        val updated = remoteUpdated.copy(id = existing.id)
        notebookDao.update(updated)
        return updated
    }
}
