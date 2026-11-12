package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.NotebookDraft
import cl.duoc.coegio_pablo_saladana.validation.NotebookValidator
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
        assertTrue(NotebookValidator.isValid(draft))
    }

    @Test
    fun isValid_rejectsMissingRequiredFields() {
        assertFalse(NotebookValidator.isValid(NotebookDraft("", "Propósito", "Instrucciones", "")))
        assertFalse(NotebookValidator.isValid(NotebookDraft("Título", " ", "Instrucciones", "")))
        assertFalse(NotebookValidator.isValid(NotebookDraft("Título", "Propósito", " ", "")))
    }

    @Test
    fun validateIdentifiesOnlyMissingFields() {
        val errors = NotebookValidator.validate(NotebookDraft("Título", " ", "Instrucciones", ""))
        assertFalse(errors.title)
        assertTrue(errors.purpose)
        assertFalse(errors.instructions)
        assertFalse(errors.isValid)
    }
}
