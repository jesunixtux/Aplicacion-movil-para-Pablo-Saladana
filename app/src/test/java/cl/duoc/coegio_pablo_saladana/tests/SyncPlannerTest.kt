package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.dto.NotebookDto
import cl.duoc.coegio_pablo_saladana.data.remote.dto.StudentEntryDto
import cl.duoc.coegio_pablo_saladana.domain.sync.ConflictResolution
import cl.duoc.coegio_pablo_saladana.domain.sync.SyncPlanner
import cl.duoc.coegio_pablo_saladana.domain.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPlannerTest {
    private fun notebook(localId: Long, remoteId: Long?, title: String = "T", syncState: String = "LOCAL_ONLY", updatedAt: Long = 100L) =
        NotebookEntity(
            id = localId, title = title, purpose = "P", instructions = "I", questions = "",
            teacherEmail = "d@e.cl", teacherName = "D", createdAt = 1L,
            remoteId = remoteId, syncState = syncState, updatedAt = updatedAt
        )

    private fun entry(localId: Long, remoteId: Long?, syncState: String = "LOCAL_ONLY", updatedAt: Long = 100L) =
        StudentEntryEntity(
            id = localId, notebookId = 1, notebookTitle = "T", studentEmail = "a@e.cl",
            studentName = "A", classDate = "18/10/2026", content = "C", activity = "A",
            learning = "L", doubts = "", isDraft = false, createdAt = 1L,
            remoteId = remoteId, syncState = syncState, updatedAt = updatedAt
        )

    @Test
    fun matchingUsesOnlyRemoteIdNeverTitle() {
        val local = listOf(notebook(1, remoteId = 42, title = "Distinto"), notebook(2, remoteId = null, title = "Igual"))
        val remote = listOf(NotebookDto(id = 42, title = "Igual"), NotebookDto(id = 99, title = "Igual"))
        val matches = SyncPlanner.matchNotebooks(local, remote)

        // Mismo título no empareja nada; solo el remoteId 42.
        assertEquals(1, matches.pairs.size)
        assertEquals(1L, matches.pairs[0].first.id)
        assertEquals(listOf(2L), matches.localOnly.map { it.id })
        assertEquals(listOf(99L), matches.remoteOnly.map { it.id })
    }

    @Test
    fun localRowWithUnknownRemoteIdStaysLocalOnly() {
        val matches = SyncPlanner.matchNotebooks(listOf(notebook(1, remoteId = 7)), listOf(NotebookDto(id = 8, title = "T")))
        assertTrue(matches.pairs.isEmpty())
        assertEquals(1, matches.localOnly.size)
        assertEquals(1, matches.remoteOnly.size)
    }

    @Test
    fun pendingUploadsExcludeSyncedRowsAndTreatUnknownStateAsLocal() {
        val rows = listOf(
            notebook(1, null, syncState = "LOCAL_ONLY"),
            notebook(2, 5, syncState = "PENDING_UPLOAD"),
            notebook(3, 6, syncState = "SYNCED"),
            notebook(4, 7, syncState = "ERROR"),
            notebook(5, null, syncState = "BASURA")
        )
        assertEquals(listOf(1L, 2L, 4L, 5L), SyncPlanner.pendingNotebookUploads(rows).map { it.id })
        assertEquals(SyncState.LOCAL_ONLY, SyncState.fromStored("BASURA"))
        assertEquals(SyncState.LOCAL_ONLY, SyncState.fromStored(null))
    }

    @Test
    fun entryMatchingAndPendingFollowTheSameRules() {
        val local = listOf(entry(1, remoteId = 10), entry(2, null, syncState = "SYNCED"))
        val remote = listOf(StudentEntryDto(id = 10), StudentEntryDto(id = 11))
        val matches = SyncPlanner.matchEntries(local, remote)
        assertEquals(1, matches.pairs.size)
        assertEquals(listOf(2L), matches.localOnly.map { it.id })
        assertEquals(listOf(11L), matches.remoteOnly.map { it.id })
        assertEquals(listOf(1L), SyncPlanner.pendingEntryUploads(local).map { it.id })
    }

    @Test
    fun conflictResolutionIsLocalFirstUntilBackendExposesUpdatedAt() {
        val local = notebook(1, 42, updatedAt = 100L)
        assertEquals(ConflictResolution.LOCAL_WINS, SyncPlanner.resolveNotebookConflict(local, NotebookDto(id = 42)))
        assertEquals(ConflictResolution.LOCAL_WINS, SyncPlanner.resolveNotebookConflict(local, NotebookDto(id = 42, updatedAt = 100L)))
        assertEquals(ConflictResolution.LOCAL_WINS, SyncPlanner.resolveNotebookConflict(local, NotebookDto(id = 42, updatedAt = 50L)))
        assertEquals(ConflictResolution.REMOTE_WINS, SyncPlanner.resolveNotebookConflict(local, NotebookDto(id = 42, updatedAt = 200L)))

        val localEntry = entry(1, 10, updatedAt = 100L)
        assertEquals(ConflictResolution.LOCAL_WINS, SyncPlanner.resolveEntryConflict(localEntry, StudentEntryDto(id = 10)))
        assertEquals(ConflictResolution.REMOTE_WINS, SyncPlanner.resolveEntryConflict(localEntry, StudentEntryDto(id = 10, updatedAt = 200L)))
    }

    @Test
    fun idempotencyCheckPreventsResendingRowsWithRemoteId() {
        assertFalse(SyncPlanner.alreadyHasRemoteIdentity(null))
        assertFalse(SyncPlanner.alreadyHasRemoteIdentity(0))
        assertTrue(SyncPlanner.alreadyHasRemoteIdentity(42))
    }
}
