package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolCatalogTest {
    @Test
    fun courseConstantsKeepTheirExactLabels() {
        assertEquals("General", SchoolCourses.GENERAL)
        assertEquals("Asignación personalizada", SchoolCourses.CUSTOM)
        assertEquals("5° Básico A", SchoolCourses.DEFAULT_STUDENT_COURSE)
    }

    @Test
    fun studentCoursesAreSixUniqueRealCourses() {
        assertEquals(6, SchoolCourses.STUDENT_COURSES.size)
        assertEquals(SchoolCourses.STUDENT_COURSES.size, SchoolCourses.STUDENT_COURSES.toSet().size)
        assertTrue(SchoolCourses.DEFAULT_STUDENT_COURSE in SchoolCourses.STUDENT_COURSES)
        assertFalse(SchoolCourses.GENERAL in SchoolCourses.STUDENT_COURSES)
        assertFalse(SchoolCourses.CUSTOM in SchoolCourses.STUDENT_COURSES)
    }

    @Test
    fun notebookCoursesReuseTheStudentCatalog() {
        assertEquals(SchoolCourses.STUDENT_COURSES, SchoolCourses.NOTEBOOK_COURSES)
    }

    @Test
    fun subjectsAreFiveUniqueAndDefaultIsIncluded() {
        assertEquals(5, SchoolSubjects.ALL.size)
        assertEquals(SchoolSubjects.ALL.size, SchoolSubjects.ALL.toSet().size)
        assertEquals("Lenguaje y Comunicación", SchoolSubjects.DEFAULT)
        assertTrue(SchoolSubjects.DEFAULT in SchoolSubjects.ALL)
        assertTrue("Matemática" in SchoolSubjects.ALL)
    }
}
