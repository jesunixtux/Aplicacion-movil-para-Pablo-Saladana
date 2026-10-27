package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.validation.StudentEntryValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentEntryDraftTest {
    @Test
    fun completeEntry_requiresDateContentActivityAndLearning() {
        assertTrue(StudentEntryValidator.isComplete(StudentEntryDraft("18/10/2026", "Tema", "Actividad", "Aprendizaje", "")))
        assertFalse(StudentEntryValidator.isComplete(StudentEntryDraft("18/10/2026", "Tema", "", "Aprendizaje", "")))
    }

    @Test
    fun completeEntry_requiresARealDateInTheExpectedFormat() {
        assertTrue(StudentEntryValidator.isComplete(StudentEntryDraft("29/02/2024", "Tema", "Actividad", "Aprendizaje", "")))
        assertFalse(StudentEntryValidator.isComplete(StudentEntryDraft("31/02/2026", "Tema", "Actividad", "Aprendizaje", "")))
        assertFalse(StudentEntryValidator.isComplete(StudentEntryDraft("1/1/2026", "Tema", "Actividad", "Aprendizaje", "")))
        assertFalse(StudentEntryValidator.isComplete(StudentEntryDraft("fecha", "Tema", "Actividad", "Aprendizaje", "")))
    }

    @Test
    fun draft_requiresAtLeastOneWrittenField() {
        assertTrue(StudentEntryValidator.hasContent(StudentEntryDraft("18/10/2026", "Tema", "", "", "")))
        assertFalse(StudentEntryValidator.hasContent(StudentEntryDraft("18/10/2026", " ", "", "", "")))
    }
}
