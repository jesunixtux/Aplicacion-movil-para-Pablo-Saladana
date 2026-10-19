package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.NotebookDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookDraftTest {
    @Test
    fun normalized_trimsFieldsAndKeepsValidDraft() {
        val draft = NotebookDraft(" Lenguaje ", " Aprender ", " Completar ", " ¿Qué aprendí? ")
            .normalized()

        assertEquals("Lenguaje", draft.title)
        assertEquals("Aprender", draft.purpose)
        assertEquals("Completar", draft.instructions)
        assertEquals("¿Qué aprendí?", draft.questions)
        assertTrue(draft.isValid())
    }

    @Test
    fun isValid_rejectsMissingRequiredFields() {
        assertFalse(NotebookDraft("", "Propósito", "Instrucciones", "").isValid())
        assertFalse(NotebookDraft("Título", " ", "Instrucciones", "").isValid())
        assertFalse(NotebookDraft("Título", "Propósito", " ", "").isValid())
    }
}
