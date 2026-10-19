package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.StudentEntryDraft
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentEntryDraftTest {
    @Test
    fun completeEntry_requiresDateContentActivityAndLearning() {
        assertTrue(StudentEntryDraft("18/10/2026", "Tema", "Actividad", "Aprendizaje", "").isComplete())
        assertFalse(StudentEntryDraft("18/10/2026", "Tema", "", "Aprendizaje", "").isComplete())
    }

    @Test
    fun draft_requiresAtLeastOneWrittenField() {
        assertTrue(StudentEntryDraft("18/10/2026", "Tema", "", "", "").hasContent())
        assertFalse(StudentEntryDraft("18/10/2026", " ", "", "", "").hasContent())
    }
}
