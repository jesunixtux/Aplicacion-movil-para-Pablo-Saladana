package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.remote.HolidayRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.NotebookRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.StudentEntryPayload
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Entradas corruptas, vacías y valores por defecto del cliente del backend. */
class RemoteDataSourcesEdgeTest {
    private val notebook = NotebookEntity(
        id = 5, title = "Historia", purpose = "P", instructions = "I", questions = "",
        teacherEmail = "d@e.cl", teacherName = "D", createdAt = 1L
    )

    // --- NotebookRemoteDataSource ---
    @Test
    fun notebookListParsingSurvivesCorruptAndEmptyJson() {
        assertTrue(NotebookRemoteDataSource.parseNotebookList("no es json").isEmpty())
        assertTrue(NotebookRemoteDataSource.parseNotebookList("null").isEmpty())
        assertTrue(NotebookRemoteDataSource.parseNotebookList("[]").isEmpty())
        assertTrue(NotebookRemoteDataSource.parseNotebookList("").isEmpty())
    }

    @Test
    fun notebooksWithoutTitleAreDropped() {
        assertTrue(NotebookRemoteDataSource.parseNotebookList("""[{"id":1,"purpose":"x"}]""").isEmpty())
        assertTrue(NotebookRemoteDataSource.parseNotebookList("""[{"id":1,"title":" "}]""").isEmpty())
    }

    @Test
    fun blankCourseFallsBackToGeneralAndZeroIdMeansNoRemoteId() {
        val parsed = NotebookRemoteDataSource.parseNotebookList("""[{"title":"T","course":" "}]""")
        assertEquals(1, parsed.size)
        assertEquals(SchoolCourses.GENERAL, parsed[0].course)
        assertNull(parsed[0].remoteId)

        val withId = NotebookRemoteDataSource.parseNotebookList("""[{"id":4,"title":"T"}]""")
        assertEquals(4L, withId[0].remoteId)
    }

    @Test
    fun singleObjectParsingRejectsCorruptJson() {
        assertNull(NotebookRemoteDataSource.parseNotebookObject("{"))
        assertNull(NotebookRemoteDataSource.parseNotebookObject("null"))
        assertNull(NotebookRemoteDataSource.parseNotebookObject("""{"purpose":"sin título"}"""))
        assertNotNull(NotebookRemoteDataSource.parseNotebookObject("""{"id":2,"title":"T"}"""))
    }

    @Test
    fun encodedInputSortsAndLowercasesAssignedEmails() {
        val encoded = NotebookRemoteDataSource.encodeNotebookInput(
            NotebookDraft("T", "P", "I", "Q", assignedStudentEmails = setOf("B@x.cl", "a@x.cl")),
            "docente@escuela.cl",
            "Docente"
        )
        assertTrue(encoded.contains("\"assignedStudentEmails\":\"a@x.cl,b@x.cl\""))
    }

    // --- HolidayRemoteDataSource ---
    @Test
    fun holidayNameFallsBackFromLocalNameToNameToFeriado() {
        val withName = HolidayRemoteDataSource.parseHolidays("""[{"date":"2026-01-01","name":"New Year"}]""")
        assertEquals("New Year", withName[0].localName)

        val withoutName = HolidayRemoteDataSource.parseHolidays("""[{"date":"2026-01-01"}]""")
        assertEquals("Feriado", withoutName[0].localName)
    }

    @Test
    fun holidaysWithoutUsableDateAreDropped() {
        assertTrue(HolidayRemoteDataSource.parseHolidays("""[{"date":" ","localName":"x"}]""").isEmpty())
        assertTrue(HolidayRemoteDataSource.parseHolidays("""[{"localName":"x"}]""").isEmpty())
        assertTrue(HolidayRemoteDataSource.parseHolidays("corrupto").isEmpty())
        assertTrue(HolidayRemoteDataSource.parseHolidays("null").isEmpty())
    }

    @Test
    fun holidayLookupAcceptsBothFormatsAndTrims() {
        val holidays = HolidayRemoteDataSource.fallbackHolidays(2026)
        assertNotNull(HolidayRemoteDataSource.findHolidayForClassDate("2026-09-18", holidays))
        assertNotNull(HolidayRemoteDataSource.findHolidayForClassDate(" 18/09/2026 ", holidays))
        assertNull(HolidayRemoteDataSource.findHolidayForClassDate("20/09/2026", holidays))
    }

    @Test
    fun fallbackCalendarHasThirteenHolidaysForTheRequestedYear() {
        val fallback = HolidayRemoteDataSource.fallbackHolidays(2027)
        assertEquals(13, fallback.size)
        assertTrue(fallback.all { it.date.startsWith("2027-") })
        assertTrue(fallback.all { Regex("\\d{2}/\\d{2}/\\d{4}").matches(it.formattedDate) })
    }

    // --- StudentEntryPayload ---
    @Test
    fun payloadWithoutOptionalFieldsUsesNotebookTitleAndEmptyStrings() {
        val entry = StudentEntryPayload(notebookId = 99).toEntity(notebook)
        assertEquals(5L, entry.notebookId)
        assertEquals("Historia", entry.notebookTitle)
        assertEquals("", entry.studentEmail)
        assertEquals("", entry.content)
        assertEquals("", entry.teacherFeedback)
        assertNull(entry.remoteId)
        assertFalse(entry.isDraft)
    }

    @Test
    fun payloadKeepsItsOwnTitleAndPositiveRemoteId() {
        val entry = StudentEntryPayload(id = 7, notebookTitle = "Otro título").toEntity(notebook)
        assertEquals("Otro título", entry.notebookTitle)
        assertEquals(7L, entry.remoteId)
    }
}
