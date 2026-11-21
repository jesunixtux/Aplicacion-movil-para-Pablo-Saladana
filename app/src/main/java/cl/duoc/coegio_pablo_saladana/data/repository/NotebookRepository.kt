package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.remote.NotebookRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.dto.NotebookDto
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.sync.SyncPlanner
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator

/** Room conserva la caché; Spring Boot es la fuente compartida cuando la identidad es remota. */
class NotebookRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val notebookDao = database.notebookDao()
    private val userDao = database.userDao()

    var lastError: String? = null
        private set

    suspend fun findAll(account: UserAccount? = null): List<NotebookEntity> = runCatching {
        lastError = null
        account?.remoteId?.let { notebookDao.findForRemoteUser(it) } ?: notebookDao.findAll()
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun syncWithBackend(account: UserAccount? = null): List<NotebookEntity> {
        val userId = account?.remoteId ?: return findAll(account)
        val remote = NotebookRemoteDataSource.fetchAll() ?: run {
            lastError = NotebookRemoteDataSource.lastError
            return findAll(account)
        }
        val plan = SyncPlanner.matchNotebooks(notebookDao.findForRemoteUser(userId), remote)
        plan.pairs.forEach { (local, dto) ->
            if (local.remoteOwnerId == userId && local.syncState != "SYNCED") uploadOwned(local, account, dto)
            else cache(dto, account)
        }
        plan.remoteOnly.forEach { cache(it, account) }
        // Solo filas creadas por esta identidad: una fila legacy sin vínculo nunca se reclama por correo.
        plan.localOnly.filter { it.remoteId == null && it.remoteOwnerId == userId }
            .forEach { uploadOwned(it, account) }
        return findAll(account)
    }

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) return fail("Solo los docentes pueden publicar bitácoras.")
        val normalized = draft.normalized()
        if (!NotebookValidator.isValid(normalized)) return fail("La bitácora tiene campos obligatorios vacíos o un periodo inválido.")
        if (normalized.course != teacher.assignedCourse || normalized.subject != teacher.assignedSubject)
            return fail("La bitácora debe usar el curso y la asignatura que tienes asignados.")
        if (findAll(teacher).any {
                it.teacherEmail == teacher.email && it.course == normalized.course &&
                    it.subject == normalized.subject && it.month == normalized.month && it.year == normalized.year
            }) return fail("Ya publicaste una bitácora de ${normalized.subject} para ${"%02d/%04d".format(java.util.Locale.ROOT, normalized.month, normalized.year)}.")

        val now = System.currentTimeMillis()
        var local = NotebookEntity(
            title = normalized.title, purpose = normalized.purpose, instructions = normalized.instructions,
            questions = normalized.questions, teacherEmail = teacher.email, teacherName = teacher.fullName,
            createdAt = now, course = normalized.course, subject = normalized.subject,
            month = normalized.month, year = normalized.year, publishedAt = now,
            assignedStudentEmails = normalized.assignedStudentEmails.sorted().joinToString(","),
            syncState = if (teacher.remoteId == null) "LOCAL_ONLY" else "PENDING_UPLOAD",
            updatedAt = now, remoteOwnerId = teacher.remoteId, cachedForUserId = teacher.remoteId
        )
        local = local.copy(id = notebookDao.insert(local))
        if (teacher.remoteId == null) return local
        uploadOwned(local, teacher) ?: return null
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun updateAssignments(
        notebookId: Long,
        teacher: UserAccount,
        course: String,
        studentEmails: Set<String>
    ): NotebookEntity? = mutate(notebookId, teacher) { existing ->
        if (existing.isClosed) return@mutate fail("La bitácora ya está cerrada; no admite cambios.")
        if (existing.course != teacher.assignedCourse || existing.subject != teacher.assignedSubject || course != teacher.assignedCourse)
            return@mutate fail("La bitácora debe mantener el curso y la asignatura que tienes asignados.")
        val students = if (teacher.remoteId == null) userDao.findAllStudents().filter { it.course == course }
            .map { it.email.lowercase() }.toSet() else studentEmails.map { it.lowercase() }.toSet()
        existing.copy(
            assignedStudentEmails = studentEmails.map(String::trim).map(String::lowercase)
                .filter { teacher.remoteId != null || it in students }.sorted().joinToString(","),
            updatedAt = if (existing.remoteId == null) System.currentTimeMillis() else existing.updatedAt,
            syncState = pendingState(teacher)
        )
    }

    suspend fun close(notebookId: Long, teacher: UserAccount): NotebookEntity? = mutate(notebookId, teacher) { existing ->
        if (existing.isClosed) return@mutate fail("La bitácora ya estaba cerrada.")
        val now = System.currentTimeMillis()
        existing.copy(
            isClosed = true, closedAt = now,
            updatedAt = if (existing.remoteId == null) now else existing.updatedAt,
            syncState = pendingState(teacher)
        )
    }

    suspend fun setRemindersEnabled(notebookId: Long, teacher: UserAccount, enabled: Boolean): NotebookEntity? =
        mutate(notebookId, teacher) { existing ->
            existing.copy(
                remindersEnabled = enabled,
                updatedAt = if (existing.remoteId == null) System.currentTimeMillis() else existing.updatedAt,
                syncState = pendingState(teacher)
            )
        }

    private suspend fun mutate(
        notebookId: Long,
        teacher: UserAccount,
        change: suspend (NotebookEntity) -> NotebookEntity?
    ): NotebookEntity? = runCatching {
        lastError = null
        if (teacher.role != UserRole.Teacher) return fail("Solo los docentes pueden modificar bitácoras.")
        val existing = notebookDao.findById(notebookId)?.takeIf {
            if (teacher.remoteId != null) it.remoteOwnerId == teacher.remoteId else it.teacherEmail.equals(teacher.email, true)
        } ?: return fail("No se encontró la bitácora o no te pertenece.")
        val candidate = change(existing) ?: return null
        notebookDao.update(candidate)
        if (teacher.remoteId == null || candidate.remoteId == null) return candidate
        val dto = when {
            !existing.isClosed && candidate.isClosed -> NotebookRemoteDataSource.close(candidate.remoteId, existing.updatedAt)
            existing.remindersEnabled != candidate.remindersEnabled ->
                NotebookRemoteDataSource.reminders(candidate.remoteId, existing.updatedAt, candidate.remindersEnabled)
            else -> NotebookRemoteDataSource.update(candidate, existing.updatedAt)
        }
        if (dto == null) {
            lastError = NotebookRemoteDataSource.lastError
            notebookDao.update(candidate.copy(syncState = "ERROR", syncError = lastError))
            return candidate
        }
        cache(dto, teacher)
    }.getOrElse { lastError = AppErrors.describe(it); null }

    private suspend fun uploadOwned(
        local: NotebookEntity,
        teacher: UserAccount,
        remoteState: NotebookDto? = null
    ): NotebookEntity? {
        var dto = if (local.remoteId == null) NotebookRemoteDataSource.create(local) else remoteState
        if (dto == null) {
            lastError = NotebookRemoteDataSource.lastError
            notebookDao.update(local.copy(syncState = "ERROR", syncError = lastError))
            return null
        }
        val remoteId = requireNotNull(dto.id.takeIf { it > 0 } ?: local.remoteId)
        if (local.remoteId != null && metadataChanged(local, dto)) {
            dto = NotebookRemoteDataSource.update(local, dto.updatedAt ?: local.updatedAt) ?: return syncFailed(local)
        }
        if (local.publishedAt > 0 && dto.publishedAt == null) {
            dto = NotebookRemoteDataSource.publish(remoteId, dto.updatedAt ?: local.updatedAt)
            if (dto == null) {
                return syncFailed(local)
            }
        }
        if (local.isClosed && dto.isClosed != true) {
            dto = NotebookRemoteDataSource.close(remoteId, dto.updatedAt ?: local.updatedAt) ?: return syncFailed(local)
        }
        if (local.remindersEnabled != dto.remindersEnabled) {
            dto = NotebookRemoteDataSource.reminders(
                remoteId, dto.updatedAt ?: local.updatedAt, local.remindersEnabled
            ) ?: return syncFailed(local)
        }
        return cache(dto, teacher)
    }

    private fun metadataChanged(local: NotebookEntity, remote: NotebookDto): Boolean =
        local.title != remote.title || local.purpose != remote.purpose ||
            local.instructions != remote.instructions || local.questions != remote.questions ||
            local.course != remote.course || local.assignedStudentEmails != remote.assignedStudentEmails ||
            local.subject != remote.subject || local.month != remote.month || local.year != remote.year

    private suspend fun syncFailed(local: NotebookEntity): NotebookEntity? {
        lastError = NotebookRemoteDataSource.lastError
        notebookDao.update(local.copy(syncState = "ERROR", syncError = lastError))
        return null
    }

    private suspend fun cache(dto: NotebookDto, account: UserAccount): NotebookEntity? {
        val existing = notebookDao.findByRemoteId(dto.id)
        val ownerId = existing?.remoteOwnerId ?: account.remoteId.takeIf { account.role == UserRole.Teacher }
        val mapped = dto.toEntity(existing?.id ?: 0, ownerId, account.remoteId)?.copy(syncState = "SYNCED", syncError = null)
            ?: return null
        return if (existing == null) mapped.copy(id = notebookDao.insert(mapped))
        else mapped.also { notebookDao.update(it) }
    }

    private fun pendingState(account: UserAccount) = if (account.remoteId == null) "LOCAL_ONLY" else "PENDING_UPLOAD"
    private fun <T> fail(message: String): T? { lastError = message; return null }
}
