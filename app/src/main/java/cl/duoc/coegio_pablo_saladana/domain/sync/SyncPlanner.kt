package cl.duoc.coegio_pablo_saladana.domain.sync

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.dto.NotebookDto
import cl.duoc.coegio_pablo_saladana.data.remote.dto.StudentEntryDto

/**
 * Planificador PURO de sincronización Room ↔ API (Tarea 3).
 *
 * Solo calcula planes: nunca escribe, borra ni modifica registros locales.
 * La correspondencia local/remota se hace EXCLUSIVAMENTE por remoteId (id de MySQL);
 * emparejar por título o correo produciría duplicados y está prohibido aquí.
 */
object SyncPlanner {
    data class NotebookMatches(
        val pairs: List<Pair<NotebookEntity, NotebookDto>>,
        val localOnly: List<NotebookEntity>,
        val remoteOnly: List<NotebookDto>
    )

    data class EntryMatches(
        val pairs: List<Pair<StudentEntryEntity, StudentEntryDto>>,
        val localOnly: List<StudentEntryEntity>,
        val remoteOnly: List<StudentEntryDto>
    )

    fun matchNotebooks(local: List<NotebookEntity>, remote: List<NotebookDto>): NotebookMatches {
        val remoteById = remote.filter { it.id > 0 }.associateBy { it.id }
        val pairs = local.mapNotNull { row -> row.remoteId?.let { id -> remoteById[id]?.let { row to it } } }
        val matchedRemoteIds = pairs.map { it.second.id }.toSet()
        return NotebookMatches(
            pairs = pairs,
            localOnly = local.filter { it.remoteId == null || it.remoteId !in remoteById },
            remoteOnly = remote.filter { it.id > 0 && it.id !in matchedRemoteIds }
        )
    }

    fun matchEntries(local: List<StudentEntryEntity>, remote: List<StudentEntryDto>): EntryMatches {
        val remoteById = remote.filter { it.id > 0 }.associateBy { it.id }
        val pairs = local.mapNotNull { row -> row.remoteId?.let { id -> remoteById[id]?.let { row to it } } }
        val matchedRemoteIds = pairs.map { it.second.id }.toSet()
        return EntryMatches(
            pairs = pairs,
            localOnly = local.filter { it.remoteId == null || it.remoteId !in remoteById },
            remoteOnly = remote.filter { it.id > 0 && it.id !in matchedRemoteIds }
        )
    }

    /** Filas que deben enviarse: nuevas, modificadas o con error previo. Nunca las SYNCED. */
    fun pendingNotebookUploads(local: List<NotebookEntity>): List<NotebookEntity> =
        local.filter { SyncState.fromStored(it.syncState) != SyncState.SYNCED }

    fun pendingEntryUploads(local: List<StudentEntryEntity>): List<StudentEntryEntity> =
        local.filter { SyncState.fromStored(it.syncState) != SyncState.SYNCED }

    /**
     * Resolución de conflictos: last-write-wins cuando el backend exponga updatedAt;
     * si no hay marca remota, gana lo local (conservador, no pierde trabajo).
     */
    fun resolveNotebookConflict(local: NotebookEntity, remote: NotebookDto): ConflictResolution =
        if (remote.updatedAt != null && remote.updatedAt > local.updatedAt) {
            ConflictResolution.REMOTE_WINS
        } else {
            ConflictResolution.LOCAL_WINS
        }

    fun resolveEntryConflict(local: StudentEntryEntity, remote: StudentEntryDto): ConflictResolution =
        if (remote.updatedAt != null && remote.updatedAt > local.updatedAt) {
            ConflictResolution.REMOTE_WINS
        } else {
            ConflictResolution.LOCAL_WINS
        }

    /**
     * Idempotencia: antes de crear en el servidor, comprobar si la fila ya tiene
     * remoteId (reintento de una subida exitosa) para no duplicar registros.
     */
    fun alreadyHasRemoteIdentity(remoteId: Long?): Boolean = remoteId != null && remoteId > 0
}
