package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.remote.dto.NotebookDto
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookDtoTest {
    private val gson = Gson()

    private fun fullEntity() = NotebookEntity(
        id = 7,
        title = "Bitácora de Ciencias",
        purpose = "Observar ecosistemas",
        instructions = "Registrar observaciones",
        questions = "¿Qué observaste?",
        teacherEmail = "docente@escuela.cl",
        teacherName = "Camila Rojas",
        createdAt = 1700000000000L,
        course = "5° Básico A",
        assignedStudentEmails = "a@escuela.cl,b@escuela.cl",
        remoteId = 42,
        subject = "Ciencias Naturales",
        month = 11,
        year = 2026,
        isClosed = true,
        publishedAt = 1700000001000L,
        closedAt = 1700000002000L,
        remindersEnabled = false,
        syncState = "PENDING_UPLOAD",
        syncError = null,
        updatedAt = 1700000003000L
    )

    @Test
    fun fullRoundTripPreservesEveryLocalField() {
        val entity = fullEntity()
        val json = gson.toJson(NotebookDto.fromEntity(entity))
        val restored = gson.fromJson(json, NotebookDto::class.java).toEntity(localId = entity.id)
        // syncState/syncError son locales y no viajan al servidor; el resto debe ser idéntico.
        assertEquals(entity.copy(syncState = "LOCAL_ONLY", syncError = null), restored)
    }

    @Test
    fun legacyBackendResponseKeepsRoomDefaults() {
        // JSON exacto que produce hoy pablo-salandra-backend (sin campos mensuales).
        val legacyJson = """
            {"id":42,"title":"T","purpose":"P","instructions":"I","questions":"Q",
             "teacherEmail":"d@e.cl","teacherName":"D","createdAt":1700000000000,
             "course":"5° Básico A","assignedStudentEmails":"a@e.cl"}
        """.trimIndent()
        val entity = gson.fromJson(legacyJson, NotebookDto::class.java).toEntity()!!
        assertEquals(42L, entity.remoteId)
        assertEquals(SchoolSubjects.DEFAULT, entity.subject)
        assertEquals(1, entity.month)
        assertEquals(2026, entity.year)
        assertFalse(entity.isClosed)
        assertEquals(0L, entity.publishedAt)
        assertNull(entity.closedAt)
        assertTrue(entity.remindersEnabled)
        assertEquals(entity.createdAt, entity.updatedAt)
    }

    @Test
    fun missingTitleProducesNoEntityAndZeroIdMeansNoRemoteId() {
        assertNull(gson.fromJson("""{"id":1,"purpose":"P"}""", NotebookDto::class.java).toEntity())
        assertNull(gson.fromJson("""{"title":" "}""", NotebookDto::class.java).toEntity())
        assertNull(gson.fromJson("""{"title":"T"}""", NotebookDto::class.java).toEntity()!!.remoteId)
        assertEquals(9L, gson.fromJson("""{"id":9,"title":"T"}""", NotebookDto::class.java).toEntity()!!.remoteId)
    }

    @Test
    fun blankCourseFallsBackToGeneralAndLocalIdIsKept() {
        val entity = gson.fromJson("""{"title":"T","course":" "}""", NotebookDto::class.java)
            .toEntity(localId = 7)!!
        assertEquals(SchoolCourses.GENERAL, entity.course)
        assertEquals(7L, entity.id)
    }

    @Test
    fun nullPublishedAtRemainsUnpublished() {
        val entity = gson.fromJson("""{"id":9,"title":"Borrador","publishedAt":null}""", NotebookDto::class.java)
            .toEntity()!!
        assertEquals(0L, entity.publishedAt)
    }

    @Test
    fun corruptJsonIsRejectedByGsonAsExpected() {
        val parsed = runCatching { gson.fromJson("{", NotebookDto::class.java) }.getOrNull()
        assertNull(parsed?.toEntity())
    }
}
