package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.remote.HolidayRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.NotebookRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.StudentEntryPayload
import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity

class RemoteDataSourcesTest {
    @Test
    fun parseNotebookList_readsSpringBootJsonPayload() {
        val json = """
            [
              {
                "id": 7,
                "title": "Ciencias Naturales",
                "purpose": "Comprender ecosistemas",
                "instructions": "Registrar observaciones",
                "questions": "¿Qué observaste?",
                "teacherEmail": "docente@example.com",
                "teacherName": "Camila Rojas",
                "createdAt": 1700000000000,
                "course": "1° Medio A",
                "assignedStudentEmails": "alumno@example.com"
              }
            ]
        """.trimIndent()

        val list = NotebookRemoteDataSource.parseNotebookList(json)
        assertEquals(1, list.size)
        assertEquals(7L, list[0].remoteId)
        assertEquals("Ciencias Naturales", list[0].title)
        assertEquals("docente@example.com", list[0].teacherEmail)
        assertEquals("1° Medio A", list[0].course)
        assertEquals("alumno@example.com", list[0].assignedStudentEmails)
    }

    @Test
    fun encodeNotebookInput_trimsFieldsAndSerializesJson() {
        val draft = NotebookDraft("  Historia ", " Analizar ", " Escribir ", " ")
        val encoded = NotebookRemoteDataSource.encodeNotebookInput(
            draft,
            " docente@example.com ",
            " Camila Rojas "
        )
        val parsed = requireNotNull(NotebookRemoteDataSource.parseNotebookObject(encoded))
        assertEquals("Historia", parsed.title)
        assertEquals("Analizar", parsed.purpose)
        assertEquals("Escribir", parsed.instructions)
        assertEquals("", parsed.questions)
        assertEquals("docente@example.com", parsed.teacherEmail)
    }

    @Test
    fun holidayDataSource_parsesFormatsAndMatchesClassDate() {
        val json = """
            [
              {"date": "2026-09-18", "localName": "Independencia Nacional"},
              {"date": "2026-12-25", "localName": "Navidad"}
            ]
        """.trimIndent()
        val holidays = HolidayRemoteDataSource.parseHolidays(json)
        assertEquals(2, holidays.size)
        assertEquals("18/09/2026", holidays[0].formattedDate)

        val matched = HolidayRemoteDataSource.findHolidayForClassDate("18/09/2026", holidays)
        assertNotNull(matched)
        assertEquals("Independencia Nacional", matched?.localName)
        assertNull(HolidayRemoteDataSource.findHolidayForClassDate("19/09/2026", holidays))
    }

    @Test
    fun fallbackHolidays_providesOfflineChileanCalendar() {
        val fallback: List<ChileanHoliday> = HolidayRemoteDataSource.fallbackHolidays(2026)
        assertTrue(fallback.isNotEmpty())
        assertNotNull(HolidayRemoteDataSource.findHolidayForClassDate("18/09/2026", fallback))
    }

    @Test
    fun studentEntryPayload_keepsRemoteIdAndReviewWhenStoredLocally() {
        val notebook = NotebookEntity(
            id = 5, title = "Historia", purpose = "Objetivo", instructions = "Escribir",
            questions = "", teacherEmail = "teacher@example.com", teacherName = "Docente",
            createdAt = 1, remoteId = 12
        )
        val entry = StudentEntryPayload(
            id = 9, notebookId = 12, studentEmail = "student@example.com",
            studentName = "Alumno", isReviewed = true, teacherFeedback = "Bien"
        ).toEntity(notebook)

        assertEquals(5L, entry.notebookId)
        assertEquals(9L, entry.remoteId)
        assertTrue(entry.isReviewed)
        assertEquals("Bien", entry.teacherFeedback)
    }
}
