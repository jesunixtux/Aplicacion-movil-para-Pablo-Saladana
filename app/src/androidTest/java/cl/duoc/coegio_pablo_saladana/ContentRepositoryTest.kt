package cl.duoc.coegio_pablo_saladana

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.repository.NotebookRepository
import cl.duoc.coegio_pablo_saladana.data.repository.StudentEntryRepository
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    @Test
    fun teacherSeesOnlySubmittedEntriesForTheirOwnNotebooks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notebooks = NotebookRepository(context)
        val entries = StudentEntryRepository(context)
        val runId = System.currentTimeMillis()
        val teacher = account(UserRole.Teacher, "teacher-review-$runId@example.com")
        val otherTeacher = account(UserRole.Teacher, "other-teacher-review-$runId@example.com")
        val student = account(UserRole.Student, "student-review-$runId@example.com")
        val ownNotebook = requireNotNull(notebooks.create(validNotebookDraft(), teacher))
        val otherNotebook = requireNotNull(notebooks.create(validNotebookDraft(), otherTeacher))
        assertNull(notebooks.create(validNotebookDraft().copy(subject = "Matemática"), teacher))
        assertNull(notebooks.create(validNotebookDraft().copy(course = "6° Básico A"), teacher))
        assertNull(notebooks.create(validNotebookDraft(), teacher)) // Periodo duplicado.

        val submitted = requireNotNull(entries.create(validEntryDraft(), false, student, ownNotebook))
        val draft = requireNotNull(entries.create(validEntryDraft(), true, student, ownNotebook))
        entries.create(validEntryDraft(), false, student, otherNotebook)

        assertEquals(listOf(submitted.id), entries.findSubmittedByTeacher(teacher.email).map { it.id })

        val updatedDraft = requireNotNull(
            entries.updateDraft(
                draft.id,
                validEntryDraft().copy(learning = "Aprendizaje completado"),
                isDraft = false,
                student = student
            )
        )
        assertEquals(false, updatedDraft.isDraft)
        assertEquals("Aprendizaje completado", updatedDraft.learning)

        val reviewed = requireNotNull(
            entries.markReviewed(submitted.id, teacher, "Muy buen trabajo")
        )
        assertEquals(true, reviewed.isReviewed)
        assertEquals("Muy buen trabajo", reviewed.teacherFeedback)
        assertNull(entries.markReviewed(submitted.id, otherTeacher, "Intento no autorizado"))

        val closed = requireNotNull(notebooks.close(ownNotebook.id, teacher))
        assertEquals(true, closed.isClosed)
        assertNull(entries.create(validEntryDraft(), false, student, closed))
        assertNull(entries.updateDraft(draft.id, validEntryDraft(), true, student))
    }

    private fun account(role: UserRole, email: String = "prueba.qa@example.com") = UserAccount(
        firstName = "Prueba",
        middleName = "",
        paternalSurname = "QA",
        maternalSurname = "Uno",
        rut = "12.345.678-5",
        email = email,
        role = role,
        gender = UserGender.Other,
        course = SchoolCourses.DEFAULT_STUDENT_COURSE,
        assignedCourse = if (role == UserRole.Teacher) SchoolCourses.DEFAULT_STUDENT_COURSE else null,
        assignedSubject = if (role == UserRole.Teacher) SchoolSubjects.DEFAULT else null
    )

    private fun validNotebookDraft() = NotebookDraft(
        "Octubre", "Aprender", "Registrar", "¿Qué aprendiste?",
        course = SchoolCourses.DEFAULT_STUDENT_COURSE,
        subject = SchoolSubjects.DEFAULT,
        month = 10, year = 2026
    )

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
