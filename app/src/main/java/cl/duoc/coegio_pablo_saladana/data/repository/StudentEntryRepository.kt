package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.StudentEntryRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceMappers
import cl.duoc.coegio_pablo_saladana.data.remote.dto.StudentEntryDto
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.sync.SyncPlanner
import cl.duoc.coegio_pablo_saladana.domain.validation.StudentEntryValidator
import java.text.SimpleDateFormat
import java.util.Locale

/** Conserva borradores en Room y comparte únicamente filas vinculadas a una identidad remota verificada. */
class StudentEntryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(context)
    private val entryDao = database.studentEntryDao()
    private val notebookDao = database.notebookDao()

    var lastError: String? = null
        private set

    suspend fun findLocalByStudent(email: String): List<StudentEntryEntity> = runCatching {
        entryDao.findByStudent(email)
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun findLocalByStudent(account: UserAccount): List<StudentEntryEntity> = runCatching {
        account.remoteId?.let { entryDao.findByRemoteOwner(it) } ?: entryDao.findByStudent(account.email)
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun findLocalSubmittedByTeacher(email: String): List<StudentEntryEntity> = runCatching {
        entryDao.findSubmittedByTeacher(email)
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun findLocalSubmittedByTeacher(account: UserAccount): List<StudentEntryEntity> = runCatching {
        account.remoteId?.let { entryDao.findSubmittedByRemoteTeacher(it) }
            ?: entryDao.findSubmittedByTeacher(account.email)
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun findByStudent(account: UserAccount): List<StudentEntryEntity> {
        if (account.remoteId == null) return findLocalByStudent(account)
        val remote = StudentEntryRemoteDataSource.fetchByStudent(account.email) ?: run {
            lastError = StudentEntryRemoteDataSource.lastError
            return findLocalByStudent(account)
        }
        val plan = SyncPlanner.matchEntries(entryDao.findByRemoteOwner(account.remoteId), remote)
        plan.pairs.forEach { (local, dto) ->
            if (local.syncState == "SYNCED") cache(dto, account) else syncOne(local, dto)
        }
        plan.remoteOnly.forEach { cache(it, account) }
        plan.localOnly.filter { it.remoteId == null && it.remoteOwnerId == account.remoteId }
            .forEach { syncOne(it) }
        return findLocalByStudent(account)
    }

    suspend fun findSubmittedByTeacher(account: UserAccount): List<StudentEntryEntity> {
        if (account.remoteId == null) return findLocalSubmittedByTeacher(account)
        val remote = StudentEntryRemoteDataSource.fetchByTeacher(account.email) ?: run {
            lastError = StudentEntryRemoteDataSource.lastError
            return findLocalSubmittedByTeacher(account)
        }
        remote.forEach { cache(it, account) }
        return findLocalSubmittedByTeacher(account)
    }

    // Compatibilidad con llamadas existentes y pruebas locales.
    suspend fun findByStudent(email: String): List<StudentEntryEntity> = findLocalByStudent(email)
    suspend fun findSubmittedByTeacher(email: String): List<StudentEntryEntity> = findLocalSubmittedByTeacher(email)

    private fun belongsToPeriod(date: String, notebook: NotebookEntity): Boolean {
        val parsed = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }.parse(date)
        }.getOrNull() ?: return false
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
        validate(student, notebook, draft, isDraft)?.let { return fail(it) }
        val normalized = draft.normalized()
        val now = System.currentTimeMillis()
        var local = StudentEntryEntity(
            notebookId = notebook.id, notebookTitle = notebook.title,
            studentEmail = student.email, studentName = student.fullName,
            classDate = normalized.classDate, content = normalized.content, activity = normalized.activity,
            learning = normalized.learning, doubts = normalized.doubts, isDraft = isDraft, createdAt = now,
            photoUris = normalized.photoUris, fileUris = normalized.fileUris, links = normalized.links,
            submittedAt = if (isDraft) null else now, updatedAt = now,
            syncState = if (student.remoteId == null) "LOCAL_ONLY" else "PENDING_UPLOAD",
            remoteOwnerId = student.remoteId
        )
        local = local.copy(id = entryDao.insert(local))
        if (student.remoteId == null) local else syncOne(local) ?: requireNotNull(entryDao.findById(local.id))
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun updateDraft(
        entryId: Long,
        draft: StudentEntryDraft,
        isDraft: Boolean,
        student: UserAccount
    ): StudentEntryEntity? = runCatching {
        lastError = null
        val existing = entryDao.findById(entryId)?.takeIf {
            it.isDraft && if (student.remoteId != null) it.remoteOwnerId == student.remoteId
            else it.studentEmail.equals(student.email, true)
        } ?: return fail("No se encontró el borrador: solo puedes editar tus propios borradores.")
        val notebook = notebookDao.findById(existing.notebookId)
            ?: return fail("La bitácora asociada al borrador ya no existe.")
        validate(student, notebook, draft, isDraft)?.let { return fail(it) }
        val normalized = draft.normalized()
        val updated = existing.copy(
            classDate = normalized.classDate, content = normalized.content, activity = normalized.activity,
            learning = normalized.learning, doubts = normalized.doubts, isDraft = isDraft,
            photoUris = normalized.photoUris, fileUris = normalized.fileUris, links = normalized.links,
            submittedAt = if (isDraft) null else System.currentTimeMillis(),
            updatedAt = if (existing.remoteId == null) System.currentTimeMillis() else existing.updatedAt,
            syncState = if (student.remoteId == null) "LOCAL_ONLY" else "PENDING_UPLOAD", syncError = null
        )
        entryDao.update(updated)
        if (student.remoteId == null) updated else syncOne(updated) ?: requireNotNull(entryDao.findById(updated.id))
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun markReviewed(entryId: Long, teacher: UserAccount, feedback: String): StudentEntryEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) return fail("Solo los docentes pueden revisar registros.")
        val existing = entryDao.findById(entryId)?.takeIf { !it.isDraft }
            ?: return fail("No se encontró el registro enviado; los borradores no se pueden revisar.")
        val notebook = notebookDao.findById(existing.notebookId)?.takeIf {
            if (teacher.remoteId != null) it.remoteOwnerId == teacher.remoteId
            else it.teacherEmail.equals(teacher.email, true)
        } ?: return fail("Solo puedes revisar registros de tus bitácoras.")
        if (teacher.remoteId != null) {
            val remoteId = existing.remoteId ?: return fail("El registro todavía no existe en el servidor.")
            val dto = StudentEntryRemoteDataSource.review(remoteId, existing.updatedAt, feedback)
                ?: return fail(StudentEntryRemoteDataSource.lastError ?: "No se pudo revisar en el servidor.")
            return cache(dto, teacher)
        }
        val now = System.currentTimeMillis()
        existing.copy(
            isReviewed = true, teacherFeedback = feedback.trim(), reviewedAt = now,
            reviewerEmail = teacher.email, updatedAt = now
        ).also { entryDao.update(it) }
    }.getOrElse { lastError = AppErrors.describe(it); null }

    private suspend fun syncOne(local: StudentEntryEntity, remoteState: StudentEntryDto? = null): StudentEntryEntity? {
        val notebook = notebookDao.findById(local.notebookId) ?: return fail("La bitácora local del registro no existe.")
        var dto = when {
            local.remoteId == null -> StudentEntryRemoteDataSource.create(local, notebook)
            remoteState != null && !remoteState.isDraft && !local.isDraft -> remoteState
            else -> StudentEntryRemoteDataSource.update(
                local, notebook, remoteState?.updatedAt ?: local.updatedAt
            )
        }
        if (dto == null) return syncFailed(local)

        var linked = mergeRemote(local, dto)
        entryDao.update(linked)
        if (dto.isDraft) {
            dto = StudentEntryRemoteDataSource.uploadEvidences(appContext, linked, requireNotNull(linked.remoteId))
                ?: return syncFailed(linked)
            linked = mergeRemote(linked, dto)
            entryDao.update(linked)
        }
        if (!local.isDraft && dto.isDraft) {
            dto = StudentEntryRemoteDataSource.submit(requireNotNull(linked.remoteId), dto.updatedAt ?: linked.updatedAt)
                ?: return syncFailed(linked)
            linked = mergeRemote(linked, dto)
        }
        return linked.copy(syncState = "SYNCED", syncError = null).also { entryDao.update(it) }
    }

    private suspend fun cache(dto: StudentEntryDto, account: UserAccount): StudentEntryEntity? {
        val notebook = notebookDao.findByRemoteId(dto.notebookId) ?: return null
        val existing = entryDao.findByRemoteId(dto.id)
        val owner = existing?.remoteOwnerId ?: account.remoteId.takeIf { account.role == UserRole.Student }
        val mapped = dto.toEntity(notebook, owner).copy(id = existing?.id ?: 0, syncState = "SYNCED", syncError = null)
        val safe = existing?.let { mergeRemote(it, dto) }?.copy(syncState = "SYNCED", syncError = null) ?: mapped
        return if (existing == null) safe.copy(id = entryDao.insert(safe)) else safe.also { entryDao.update(it) }
    }

    /** Mantiene content:// locales y agrega referencias remotas descargables sin duplicarlas. */
    private suspend fun mergeRemote(local: StudentEntryEntity, dto: StudentEntryDto): StudentEntryEntity {
        val notebook = notebookDao.findByRemoteId(dto.notebookId)
            ?: notebookDao.findById(local.notebookId)
            ?: error("La bitácora del registro remoto no existe en Room")
        val remote = dto.toEntity(notebook = notebook, remoteOwnerId = local.remoteOwnerId)
        return remote.copy(
            id = local.id,
            photoUris = mergeLines(
                local.photoUris,
                EvidenceMappers.toLocalPhotoLines(dto.evidences).takeUnless { local.photoUris.contains("content://") }.orEmpty()
            ),
            fileUris = mergeLines(
                local.fileUris,
                EvidenceMappers.toLocalFileLines(dto.evidences).takeUnless { local.fileUris.contains("content://") }.orEmpty()
            ),
            links = mergeLines(local.links, EvidenceMappers.toLocalLinkLines(dto.evidences)),
            remoteOwnerId = local.remoteOwnerId
        )
    }

    private fun mergeLines(first: String, second: String): String =
        (first.lineSequence() + second.lineSequence()).map(String::trim).filter(String::isNotBlank).distinct().joinToString("\n")

    private suspend fun syncFailed(entry: StudentEntryEntity): StudentEntryEntity? {
        lastError = StudentEntryRemoteDataSource.lastError
        entryDao.update(entry.copy(
            isDraft = true,
            submittedAt = null,
            syncState = "ERROR",
            syncError = lastError
        ))
        return null
    }

    private fun validate(student: UserAccount, notebook: NotebookEntity, draft: StudentEntryDraft, isDraft: Boolean): String? {
        if (student.role != UserRole.Student) return "Solo los estudiantes pueden crear registros de clase."
        if (!notebook.isOpen) return "La bitácora está cerrada; ya no admite nuevos registros."
        if (student.remoteId == null && !notebook.isAccessibleBy(student)) return "No tienes acceso a esta bitácora."
        val normalized = draft.normalized()
        if (!StudentEntryValidator.hasContent(normalized)) return "Escribe al menos un dato antes de guardar el registro."
        if (!isDraft && !StudentEntryValidator.isComplete(normalized)) return "Faltan campos obligatorios para enviar."
        if (!isDraft && !belongsToPeriod(normalized.classDate, notebook))
            return "La fecha de la clase debe estar dentro del periodo ${notebook.periodLabel} de la bitácora."
        return null
    }

    private fun <T> fail(message: String): T? { lastError = message; return null }
}
