package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator

/** Etapa Android del MVP: guarda bitácoras en Room. La sincronización remota se integrará después. */
class NotebookRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val notebookDao = database.notebookDao()
    private val userDao = database.userDao()

    suspend fun findAll(): List<NotebookEntity> = notebookDao.findAll()

    /** Mientras el backend no admita el modelo mensual, Room es la fuente de datos. */
    suspend fun syncWithBackend(): List<NotebookEntity> {
        return notebookDao.findAll()
    }

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null

        val normalizedDraft = draft.normalized()
        if (!NotebookValidator.isValid(normalizedDraft) ||
            normalizedDraft.course != teacher.assignedCourse ||
            normalizedDraft.subject != teacher.assignedSubject) return null
        if (notebookDao.findAll().any {
                it.teacherEmail == teacher.email && it.course == normalizedDraft.course &&
                    it.subject == normalizedDraft.subject && it.month == normalizedDraft.month &&
                    it.year == normalizedDraft.year
            }) return null
        val now = System.currentTimeMillis()
        val notebook = NotebookEntity(
            title = normalizedDraft.title,
            purpose = normalizedDraft.purpose,
            instructions = normalizedDraft.instructions,
            questions = normalizedDraft.questions,
            teacherEmail = teacher.email,
            teacherName = teacher.fullName,
            createdAt = now,
            course = normalizedDraft.course,
            subject = normalizedDraft.subject,
            month = normalizedDraft.month,
            year = normalizedDraft.year,
            publishedAt = now,
            assignedStudentEmails = normalizedDraft.assignedStudentEmails.joinToString(",")
        )
        return notebook.copy(id = notebookDao.insert(notebook))
    }

    /** Solo el docente responsable puede actualizar la lista de estudiantes de su curso. */
    suspend fun updateAssignments(
        notebookId: Long,
        teacher: UserAccount,
        course: String,
        studentEmails: Set<String>
    ): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null
        val existing = notebookDao.findById(notebookId)
            ?.takeIf { it.teacherEmail.equals(teacher.email, ignoreCase = true) } ?: return null
        if (existing.isClosed || existing.course != teacher.assignedCourse ||
            existing.subject != teacher.assignedSubject || course != teacher.assignedCourse) return null
        val normalizedCourse = teacher.assignedCourse ?: return null
        val studentsInCourse = userDao.findAllStudents()
            .filter { it.course == normalizedCourse }
            .map { it.email.lowercase() }.toSet()
        val serializedEmails = studentEmails
            .map { it.trim().lowercase() }
            .filter { it in studentsInCourse }
            .sorted()
            .joinToString(",")
        val updated = existing.copy(
            course = normalizedCourse,
            assignedStudentEmails = serializedEmails
        )
        notebookDao.update(updated)
        return updated
    }

    suspend fun close(notebookId: Long, teacher: UserAccount): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null
        val existing = notebookDao.findById(notebookId) ?: return null
        if (existing.isClosed || existing.teacherEmail != teacher.email ||
            existing.course != teacher.assignedCourse || existing.subject != teacher.assignedSubject) return null
        val closed = existing.copy(isClosed = true, closedAt = System.currentTimeMillis())
        notebookDao.update(closed)
        return closed
    }

    suspend fun setRemindersEnabled(notebookId: Long, teacher: UserAccount, enabled: Boolean): NotebookEntity? {
        if (teacher.role != UserRole.Teacher) return null
        val existing = notebookDao.findById(notebookId) ?: return null
        if (existing.teacherEmail != teacher.email || existing.course != teacher.assignedCourse ||
            existing.subject != teacher.assignedSubject) return null
        val updated = existing.copy(remindersEnabled = enabled)
        notebookDao.update(updated)
        return updated
    }
}
