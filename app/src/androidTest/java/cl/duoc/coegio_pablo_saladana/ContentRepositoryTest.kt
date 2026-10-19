package cl.duoc.coegio_pablo_saladana

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.coegio_pablo_saladana.data.NotebookDraft
import cl.duoc.coegio_pablo_saladana.data.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.NotebookRepository
import cl.duoc.coegio_pablo_saladana.data.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.data.StudentEntryRepository
import cl.duoc.coegio_pablo_saladana.data.UserAccount
import cl.duoc.coegio_pablo_saladana.data.UserGender
import cl.duoc.coegio_pablo_saladana.data.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentRepositoryTest {
    @Test
    fun onlyTheCorrectRoleCanCreateContent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notebookRepository = NotebookRepository(context)
        val entryRepository = StudentEntryRepository(context)

        assertNull(notebookRepository.create(validNotebookDraft(), account(UserRole.Student)))
        assertNull(entryRepository.create(validEntryDraft(), false, account(UserRole.Teacher), notebook()))
    }

    private fun account(role: UserRole) = UserAccount(
        firstName = "Prueba",
        middleName = "",
        paternalSurname = "QA",
        maternalSurname = "Uno",
        rut = "12.345.678-5",
        email = "prueba.qa@example.com",
        role = role,
        gender = UserGender.Other
    )

    private fun validNotebookDraft() = NotebookDraft("Octubre", "Aprender", "Registrar", "")

    private fun validEntryDraft() = StudentEntryDraft(
        classDate = "19/10/2026",
        content = "Contenido",
        activity = "Actividad",
        learning = "Aprendizaje",
        doubts = ""
    )

    private fun notebook() = NotebookEntity(
        id = 1,
        title = "Octubre",
        purpose = "Aprender",
        instructions = "Registrar",
        questions = "",
        teacherEmail = "docente@example.com",
        teacherName = "Docente QA",
        createdAt = 0
    )
}
