package cl.duoc.coegio_pablo_saladana

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.coegio_pablo_saladana.data.DebugDataSeeder
import cl.duoc.coegio_pablo_saladana.data.NotebookRepository
import cl.duoc.coegio_pablo_saladana.data.StudentEntryRepository
import cl.duoc.coegio_pablo_saladana.data.UserRepository
import cl.duoc.coegio_pablo_saladana.data.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugDataSeederTest {
    @Test
    fun createsDemoUsersNotebookAndStudentEntriesWithoutDuplicates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val users = UserRepository(context)
        val notebooks = NotebookRepository(context)
        val entries = StudentEntryRepository(context)

        DebugDataSeeder.seedIfNeeded(context, users, notebooks, entries)

        assertEquals(UserRole.Teacher, users.findAccountByEmail("docente.prueba@example.com")?.role)
        assertEquals(UserRole.Student, users.findAccountByEmail("estudiante.prueba@example.com")?.role)
        assertEquals(UserRole.Student, users.findAccountByEmail("tercer.estudiante@example.com")?.role)

        val notebook = notebooks.findAll().firstOrNull {
            it.title == "Demostración: ciclo del agua" && it.teacherEmail == "docente.prueba@example.com"
        }
        assertNotNull(notebook)
        val seededNotebook = requireNotNull(notebook)

        val studentEmails = listOf("estudiante.prueba@example.com", "tercer.estudiante@example.com")
        val countsBefore = studentEmails.associateWith { email ->
            entries.findByStudent(email).count { it.notebookId == seededNotebook.id }
        }
        assertTrue(countsBefore.values.all { it >= 2 })
        studentEmails.forEach { email ->
            val studentEntries = entries.findByStudent(email).filter { it.notebookId == seededNotebook.id }
            assertTrue(studentEntries.any { !it.isDraft })
            assertTrue(studentEntries.any { it.isDraft })
        }

        DebugDataSeeder.seedIfNeeded(context, users, notebooks, entries)

        val countsAfter = studentEmails.associateWith { email ->
            entries.findByStudent(email).count { it.notebookId == seededNotebook.id }
        }
        assertEquals(countsBefore, countsAfter)
        assertEquals(
            1,
            notebooks.findAll().count {
                it.title == "Demostración: ciclo del agua" && it.teacherEmail == "docente.prueba@example.com"
            }
        )
    }
}
