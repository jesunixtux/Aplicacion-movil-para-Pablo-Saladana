package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.ui.AppUiState
import cl.duoc.coegio_pablo_saladana.ui.auth.LoginUiState
import cl.duoc.coegio_pablo_saladana.ui.auth.RegisterUiState
import cl.duoc.coegio_pablo_saladana.ui.student.StudentEntryUiState
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherNotebookUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateDefaultsTest {
    @Test
    fun emptyLoginFormStartsFlaggingBothFields() {
        val state = LoginUiState()
        assertTrue(state.errors.email)
        assertTrue(state.errors.password)
        assertFalse(state.errors.isValid)
        assertFalse(state.passwordVisible)
        assertFalse(state.isLoading)
        assertFalse(state.attemptedSubmit)
        assertNull(state.message)
    }

    @Test
    fun emptyRegisterFormFlagsEverythingExceptOptionalMiddleName() {
        val errors = RegisterUiState().errors
        assertTrue(errors.firstName)
        assertFalse(errors.middleName)
        assertTrue(errors.paternalSurname)
        assertTrue(errors.maternalSurname)
        assertTrue(errors.rut)
        assertTrue(errors.email)
        assertTrue(errors.password)
        assertTrue(errors.confirmPassword)
        assertFalse(errors.isValid)
    }

    @Test
    fun emptyNotebookFormFlagsTextsButKeepsValidDefaults() {
        val errors = TeacherNotebookUiState().errors
        assertTrue(errors.title)
        assertTrue(errors.purpose)
        assertTrue(errors.instructions)
        assertTrue(errors.questions)
        assertFalse(errors.course)
        assertFalse(errors.subject)
        assertFalse(errors.period)
    }

    @Test
    fun emptyEntryFormStartsWithNoFieldErrors() {
        assertTrue(StudentEntryUiState().errors.isValid)
    }

    @Test
    fun appStateResolvesSelectedNotebookAndEditingEntryById() {
        val notebook = NotebookEntity(
            id = 7, title = "T", purpose = "P", instructions = "I", questions = "",
            teacherEmail = "d@e.cl", teacherName = "D", createdAt = 1L
        )
        val entry = StudentEntryEntity(
            id = 9, notebookId = 7, notebookTitle = "T", studentEmail = "a@e.cl",
            studentName = "A", classDate = "18/10/2026", content = "C", activity = "A",
            learning = "L", doubts = "", isDraft = true, createdAt = 1L
        )
        val state = AppUiState(
            notebooks = listOf(notebook),
            studentEntries = listOf(entry),
            selectedNotebookId = 7,
            editingEntryId = 9
        )
        assertEquals(notebook, state.selectedNotebook)
        assertEquals(entry, state.editingEntry)

        val missing = state.copy(selectedNotebookId = 99, editingEntryId = 99)
        assertNull(missing.selectedNotebook)
        assertNull(missing.editingEntry)
        assertNull(AppUiState().selectedNotebook)
        assertNull(AppUiState().editingEntry)
    }
}
