package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceKind
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceTransferState
import cl.duoc.coegio_pablo_saladana.data.remote.dto.StudentEntryDto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentEntryDtoTest {
    private val gson = Gson()

    private val notebook = NotebookEntity(
        id = 5, title = "Historia", purpose = "P", instructions = "I", questions = "",
        teacherEmail = "d@e.cl", teacherName = "D", createdAt = 1L, remoteId = 42
    )

    private fun fullEntry() = StudentEntryEntity(
        id = 9, notebookId = 5, notebookTitle = "Historia",
        studentEmail = "a@e.cl", studentName = "Alumno",
        classDate = "18/10/2026", content = "Tema", activity = "Actividad",
        learning = "Aprendizaje", doubts = "Duda", isDraft = false, createdAt = 100L,
        isReviewed = true, teacherFeedback = "Bien", remoteId = 77,
        photoUris = "https://cdn.example.org/foto1.jpg",
        fileUris = "https://cdn.example.org/doc1.pdf",
        links = "https://example.org/recurso",
        submittedAt = 200L, reviewedAt = 300L, reviewerEmail = "d@e.cl",
        updatedAt = 400L
    )

    @Test
    fun roundTripWithRemoteReferencesPreservesEverything() {
        val entry = fullEntry()
        val json = gson.toJson(StudentEntryDto.fromEntity(entry, remoteNotebookId = 42))
        val dto = gson.fromJson(json, StudentEntryDto::class.java)
        assertEquals(42L, dto.notebookId)
        val restored = dto.toEntity(notebook)
        // El id local y los estados de sync son locales; el resto debe coincidir.
        assertEquals(entry.copy(id = 0, syncState = "LOCAL_ONLY"), restored)
    }

    @Test
    fun legacyBackendResponseDerivesSubmissionDateLikeRoomMigration() {
        val legacyJson = """
            {"id":77,"notebookId":42,"notebookTitle":"Historia","teacherEmail":"d@e.cl",
             "studentEmail":"a@e.cl","studentName":"Alumno","classDate":"18/10/2026",
             "content":"Tema","activity":"Actividad","learning":"Aprendizaje","doubts":"",
             "isDraft":false,"createdAt":100,"isReviewed":true,"teacherFeedback":"Bien"}
        """.trimIndent()
        val entity = gson.fromJson(legacyJson, StudentEntryDto::class.java).toEntity(notebook)
        assertEquals(5L, entity.notebookId) // id LOCAL, no el remoto 42
        assertEquals(77L, entity.remoteId)
        assertEquals(100L, entity.submittedAt) // enviado sin fecha explícita → createdAt
        assertNull(entity.reviewedAt)
        assertEquals("", entity.reviewerEmail)
        assertEquals("", entity.photoUris)
    }

    @Test
    fun legacyDraftHasNoSubmissionDate() {
        val dto = gson.fromJson("""{"id":1,"isDraft":true,"createdAt":100}""", StudentEntryDto::class.java)
        assertNull(dto.toEntity(notebook).submittedAt)
    }

    @Test
    fun localContentUrisNeverTravelToTheServer() {
        val entry = fullEntry().copy(
            photoUris = "content://media/external/images/1",
            fileUris = "content://docs/1",
            links = "https://example.org"
        )
        val dto = StudentEntryDto.fromEntity(entry, remoteNotebookId = 42)
        val json = gson.toJson(dto)
        assertFalse(json.contains("content://"))

        val photos = dto.evidences!!.filter { it.kind == EvidenceKind.PHOTO }
        assertEquals(1, photos.size)
        assertEquals(EvidenceTransferState.PENDING, photos[0].transferState)
        assertNull(photos[0].remoteRef)
        assertNull(photos[0].url)
        assertFalse(photos[0].isShareable)

        // Al volver a Room, lo pendiente no se escribe como referencia falsa.
        val restored = dto.toEntity(notebook)
        assertEquals("", restored.photoUris)
        assertEquals("", restored.fileUris)
        assertEquals("https://example.org", restored.links)
    }

    @Test
    fun zeroIdMeansNoRemoteIdentity() {
        val entity = gson.fromJson("""{"notebookId":42,"content":"x"}""", StudentEntryDto::class.java)
            .toEntity(notebook)
        assertNull(entity.remoteId)
        assertEquals("Historia", entity.notebookTitle) // respaldo del título local
    }
}
