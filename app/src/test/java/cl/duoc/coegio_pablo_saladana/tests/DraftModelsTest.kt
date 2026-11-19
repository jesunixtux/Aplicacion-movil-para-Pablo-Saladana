package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftModelsTest {
    @Test
    fun notebookNormalizedTrimsEveryTextField() {
        val draft = NotebookDraft(
            title = " Título ",
            purpose = " Propósito ",
            instructions = " Instrucciones ",
            questions = " Preguntas ",
            course = " 5° Básico A ",
            subject = " Matemática "
        ).normalized()

        assertEquals("Título", draft.title)
        assertEquals("Propósito", draft.purpose)
        assertEquals("Instrucciones", draft.instructions)
        assertEquals("Preguntas", draft.questions)
        assertEquals("5° Básico A", draft.course)
        assertEquals("Matemática", draft.subject)
    }

    @Test
    fun notebookNormalizedLowercasesTrimsAndDeduplicatesEmails() {
        val draft = NotebookDraft(
            "T", "P", "I", "Q",
            assignedStudentEmails = setOf(" Ana@X.cl ", "", "ana@x.cl", "B@Y.CL")
        ).normalized()

        assertEquals(setOf("ana@x.cl", "b@y.cl"), draft.assignedStudentEmails)
    }

    @Test
    fun notebookDefaultsUseTheCurrentValidPeriod() {
        val draft = NotebookDraft("T", "P", "I", "Q")
        assertTrue(draft.month in 1..12)
        assertTrue(draft.year in 2020..2100)
        val normalized = draft.normalized()
        assertEquals(draft.month, normalized.month)
        assertEquals(draft.year, normalized.year)
    }

    @Test
    fun studentEntryNormalizedTrimsAllEightFields() {
        val draft = StudentEntryDraft(
            classDate = " 18/10/2026 ",
            content = " Tema ",
            activity = " Actividad ",
            learning = " Aprendizaje ",
            doubts = " Duda ",
            photoUris = " content://foto/1 ",
            fileUris = " content://doc/1 ",
            links = " https://example.org "
        ).normalized()

        assertEquals("18/10/2026", draft.classDate)
        assertEquals("Tema", draft.content)
        assertEquals("Actividad", draft.activity)
        assertEquals("Aprendizaje", draft.learning)
        assertEquals("Duda", draft.doubts)
        assertEquals("content://foto/1", draft.photoUris)
        assertEquals("content://doc/1", draft.fileUris)
        assertEquals("https://example.org", draft.links)
    }
}
