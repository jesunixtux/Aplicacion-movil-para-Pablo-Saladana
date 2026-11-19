package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.StudentEntryValidator
import java.text.SimpleDateFormat
import java.util.Locale

/** Gestiona los registros de clase y evita que cuentas docentes los creen como estudiantes. */
class StudentEntryRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val entryDao = database.studentEntryDao()
    private val notebookDao = database.notebookDao()

    /** Motivo específico del último fallo; null si la última operación fue exitosa. */
    var lastError: String? = null
        private set

    suspend fun findLocalByStudent(email: String): List<StudentEntryEntity> = runCatching {
        lastError = null
        entryDao.findByStudent(email)
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun findLocalSubmittedByTeacher(email: String): List<StudentEntryEntity> = runCatching {
        lastError = null
        entryDao.findSubmittedByTeacher(email)
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun findByStudent(email: String): List<StudentEntryEntity> = findLocalByStudent(email)

    suspend fun findSubmittedByTeacher(email: String): List<StudentEntryEntity> = findLocalSubmittedByTeacher(email)

    private fun belongsToPeriod(date: String, notebook: NotebookEntity): Boolean {
        val parser = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }
        val parsed = runCatching { parser.parse(date) }.getOrNull() ?: return false
        val calendar = java.util.Calendar.getInstance().apply { time = parsed }
        return calendar.get(java.util.Calendar.MONTH) + 1 == notebook.month &&
            calendar.get(java.util.Calendar.YEAR) == notebook.year
    }

    suspend fun create(
        draft: StudentEntryDraft,
        isDraft: Boolean,
        student: UserAccount,
        notebook: NotebookEntity
    ): StudentEntryEntity? = runCatching {
        lastError = null
        if (student.role != UserRole.Student) {
            lastError = "Solo los estudiantes pueden crear registros de clase."
            return null
        }
        if (!notebook.isOpen) {
            lastError = "La bitácora está cerrada; ya no admite nuevos registros."
            return null
        }
        if (!notebook.isAccessibleBy(student)) {
            lastError = "No tienes acceso a esta bitácora: no es de tu curso o no estás en la lista de invitados."
            return null
        }

        val normalizedDraft = draft.normalized()
        if (!StudentEntryValidator.hasContent(normalizedDraft)) {
            lastError = "Escribe al menos un dato antes de guardar el registro."
            return null
        }
        if (!isDraft && !StudentEntryValidator.isComplete(normalizedDraft)) {
            lastError = "Faltan campos obligatorios (fecha, tema, actividad, aprendizaje o enlaces válidos) para enviar."
            return null
        }
        if (!isDraft && !belongsToPeriod(normalizedDraft.classDate, notebook)) {
            lastError = "La fecha de la clase debe estar dentro del periodo ${notebook.periodLabel} de la bitácora."
            return null
        }

        val now = System.currentTimeMillis()

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
            createdAt = now,
            photoUris = normalizedDraft.photoUris,
            fileUris = normalizedDraft.fileUris,
            links = normalizedDraft.links,
            submittedAt = if (isDraft) null else now,
            updatedAt = now
        )
        entry.copy(id = entryDao.insert(entry))
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun updateDraft(
        entryId: Long,
        draft: StudentEntryDraft,
        isDraft: Boolean,
        student: UserAccount
    ): StudentEntryEntity? = runCatching {
        lastError = null
        if (student.role != UserRole.Student) {
            lastError = "Solo los estudiantes pueden editar registros de clase."
            return null
        }
        val normalizedDraft = draft.normalized()
        if (!StudentEntryValidator.hasContent(normalizedDraft)) {
            lastError = "Escribe al menos un dato antes de guardar el registro."
            return null
        }
        if (!isDraft && !StudentEntryValidator.isComplete(normalizedDraft)) {
            lastError = "Faltan campos obligatorios (fecha, tema, actividad, aprendizaje o enlaces válidos) para enviar."
            return null
        }

        val existing = entryDao.findById(entryId)
            ?.takeIf { it.isDraft && it.studentEmail.equals(student.email, ignoreCase = true) } ?: run {
                lastError = "No se encontró el borrador: solo puedes editar tus propios borradores."
                return null
            }
        val notebook = notebookDao.findById(existing.notebookId) ?: run {
            lastError = "La bitácora asociada al borrador ya no existe."
            return null
        }
        if (!notebook.isOpen) {
            lastError = "La bitácora está cerrada; ya no admite cambios."
            return null
        }
        if (!notebook.isAccessibleBy(student)) {
            lastError = "Perdiste el acceso a esta bitácora: no es de tu curso o no estás invitado."
            return null
        }
        if (!isDraft && !belongsToPeriod(normalizedDraft.classDate, notebook)) {
            lastError = "La fecha de la clase debe estar dentro del periodo ${notebook.periodLabel} de la bitácora."
            return null
        }
        val candidate = existing.copy(
            classDate = normalizedDraft.classDate,
            content = normalizedDraft.content,
            activity = normalizedDraft.activity,
            learning = normalizedDraft.learning,
            doubts = normalizedDraft.doubts,
            isDraft = isDraft,
            photoUris = normalizedDraft.photoUris,
            fileUris = normalizedDraft.fileUris,
            links = normalizedDraft.links,
            submittedAt = if (isDraft) null else System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        entryDao.update(candidate)
        candidate
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun markReviewed(
        entryId: Long,
        teacher: UserAccount,
        feedback: String
    ): StudentEntryEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) {
            lastError = "Solo los docentes pueden revisar registros."
            return null
        }
        val existing = entryDao.findById(entryId)?.takeIf { !it.isDraft } ?: run {
            lastError = "No se encontró el registro enviado; los borradores no se pueden revisar."
            return null
        }
        val notebook = notebookDao.findById(existing.notebookId)
            ?.takeIf { it.teacherEmail.equals(teacher.email, ignoreCase = true) &&
                it.course == teacher.assignedCourse && it.subject == teacher.assignedSubject } ?: run {
                lastError = "Solo puedes revisar registros de bitácoras de tu curso y asignatura."
                return null
            }
        val updated = existing.copy(
            isReviewed = true, teacherFeedback = feedback.trim(),
            reviewedAt = System.currentTimeMillis(), reviewerEmail = teacher.email,
            updatedAt = System.currentTimeMillis()
        )
        entryDao.update(updated)
        updated
    }.getOrElse { lastError = AppErrors.describe(it); null }
}
