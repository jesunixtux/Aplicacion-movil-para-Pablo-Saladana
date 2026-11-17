package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
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

    suspend fun findLocalByStudent(email: String): List<StudentEntryEntity> = entryDao.findByStudent(email)

    suspend fun findLocalSubmittedByTeacher(email: String): List<StudentEntryEntity> =
        entryDao.findSubmittedByTeacher(email)

    suspend fun findByStudent(email: String): List<StudentEntryEntity> = entryDao.findByStudent(email)

    suspend fun findSubmittedByTeacher(email: String): List<StudentEntryEntity> = entryDao.findSubmittedByTeacher(email)

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
    ): StudentEntryEntity? {
        if (student.role != UserRole.Student) return null
        if (!notebook.isOpen || !notebook.isAccessibleBy(student)) return null

        val normalizedDraft = draft.normalized()
        if (!StudentEntryValidator.hasContent(normalizedDraft) ||
            (!isDraft && (!StudentEntryValidator.isComplete(normalizedDraft) ||
                !belongsToPeriod(normalizedDraft.classDate, notebook)))) return null

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
            submittedAt = if (isDraft) null else now
        )
        return entry.copy(id = entryDao.insert(entry))
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
        if (!notebook.isOpen || !notebook.isAccessibleBy(student) ||
            (!isDraft && !belongsToPeriod(normalizedDraft.classDate, notebook))) return null
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
            submittedAt = if (isDraft) null else System.currentTimeMillis()
        )
        entryDao.update(candidate)
        return candidate
    }

    suspend fun markReviewed(
        entryId: Long,
        teacher: UserAccount,
        feedback: String
    ): StudentEntryEntity? {
        if (teacher.role != UserRole.Teacher) return null
        val existing = entryDao.findById(entryId)?.takeIf { !it.isDraft } ?: return null
        val notebook = notebookDao.findById(existing.notebookId)
            ?.takeIf { it.teacherEmail.equals(teacher.email, ignoreCase = true) &&
                it.course == teacher.assignedCourse && it.subject == teacher.assignedSubject } ?: return null
        val updated = existing.copy(
            isReviewed = true, teacherFeedback = feedback.trim(),
            reviewedAt = System.currentTimeMillis(), reviewerEmail = teacher.email
        )
        entryDao.update(updated)
        return updated
    }
}
