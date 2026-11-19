package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator

/** Etapa Android del MVP: guarda bitácoras en Room. La sincronización remota se integrará después. */
class NotebookRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val notebookDao = database.notebookDao()
    private val userDao = database.userDao()

    /** Motivo específico del último fallo; null si la última operación fue exitosa. */
    var lastError: String? = null
        private set

    suspend fun findAll(): List<NotebookEntity> = runCatching {
        lastError = null
        notebookDao.findAll()
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    /** Mientras el backend no admita el modelo mensual, Room es la fuente de datos. */
    suspend fun syncWithBackend(): List<NotebookEntity> = findAll()

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) {
            lastError = "Solo los docentes pueden publicar bitácoras."
            return null
        }

        val normalizedDraft = draft.normalized()
        if (!NotebookValidator.isValid(normalizedDraft)) {
            lastError = "La bitácora tiene campos obligatorios vacíos o un periodo inválido."
            return null
        }
        if (normalizedDraft.course != teacher.assignedCourse || normalizedDraft.subject != teacher.assignedSubject) {
            lastError = "La bitácora debe usar el curso y la asignatura que tienes asignados."
            return null
        }
        if (notebookDao.findAll().any {
                it.teacherEmail == teacher.email && it.course == normalizedDraft.course &&
                    it.subject == normalizedDraft.subject && it.month == normalizedDraft.month &&
                    it.year == normalizedDraft.year
            }) {
            lastError = "Ya publicaste una bitácora de ${normalizedDraft.subject} para ${"%02d/%04d".format(java.util.Locale.ROOT, normalizedDraft.month, normalizedDraft.year)}."
            return null
        }
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
            assignedStudentEmails = normalizedDraft.assignedStudentEmails.joinToString(","),
            updatedAt = now
        )
        notebook.copy(id = notebookDao.insert(notebook))
    }.getOrElse { lastError = AppErrors.describe(it); null }

    /** Solo el docente responsable puede actualizar la lista de estudiantes de su curso. */
    suspend fun updateAssignments(
        notebookId: Long,
        teacher: UserAccount,
        course: String,
        studentEmails: Set<String>
    ): NotebookEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) {
            lastError = "Solo los docentes pueden asignar estudiantes."
            return null
        }
        val existing = notebookDao.findById(notebookId)
            ?.takeIf { it.teacherEmail.equals(teacher.email, ignoreCase = true) } ?: run {
                lastError = "No se encontró la bitácora o no te pertenece."
                return null
            }
        if (existing.isClosed) {
            lastError = "La bitácora ya está cerrada; no admite cambios."
            return null
        }
        if (existing.course != teacher.assignedCourse ||
            existing.subject != teacher.assignedSubject || course != teacher.assignedCourse) {
            lastError = "La bitácora debe mantener el curso y la asignatura que tienes asignados."
            return null
        }
        val normalizedCourse = teacher.assignedCourse ?: run {
            lastError = "No tienes un curso asignado todavía."
            return null
        }
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
            assignedStudentEmails = serializedEmails,
            updatedAt = System.currentTimeMillis()
        )
        notebookDao.update(updated)
        updated
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun close(notebookId: Long, teacher: UserAccount): NotebookEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) {
            lastError = "Solo los docentes pueden cerrar bitácoras."
            return null
        }
        val existing = notebookDao.findById(notebookId) ?: run {
            lastError = "No se encontró la bitácora que quieres cerrar."
            return null
        }
        if (existing.isClosed) {
            lastError = "La bitácora ya estaba cerrada."
            return null
        }
        if (existing.teacherEmail != teacher.email ||
            existing.course != teacher.assignedCourse || existing.subject != teacher.assignedSubject) {
            lastError = "Solo puedes cerrar bitácoras de tu curso y asignatura."
            return null
        }
        val closed = existing.copy(isClosed = true, closedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        notebookDao.update(closed)
        closed
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun setRemindersEnabled(notebookId: Long, teacher: UserAccount, enabled: Boolean): NotebookEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) {
            lastError = "Solo los docentes pueden cambiar los recordatorios."
            return null
        }
        val existing = notebookDao.findById(notebookId) ?: run {
            lastError = "No se encontró la bitácora."
            return null
        }
        if (existing.teacherEmail != teacher.email || existing.course != teacher.assignedCourse ||
            existing.subject != teacher.assignedSubject) {
            lastError = "Solo puedes cambiar recordatorios de tus propias bitácoras."
            return null
        }
        val updated = existing.copy(remindersEnabled = enabled, updatedAt = System.currentTimeMillis())
        notebookDao.update(updated)
        updated
    }.getOrElse { lastError = AppErrors.describe(it); null }
}
