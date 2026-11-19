package cl.duoc.coegio_pablo_saladana.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount

/** Bitácora publicada por un docente y visible para los estudiantes según curso o asignación directa. */
@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val purpose: String,
    val instructions: String,
    val questions: String,
    val teacherEmail: String,
    val teacherName: String,
    val createdAt: Long,
    val course: String = SchoolCourses.GENERAL,
    val assignedStudentEmails: String = "",
    val remoteId: Long? = null,
    val subject: String = SchoolSubjects.DEFAULT,
    val month: Int = 1,
    val year: Int = 2026,
    val isClosed: Boolean = false,
    val publishedAt: Long = createdAt,
    val closedAt: Long? = null,
    val remindersEnabled: Boolean = true,
    // --- Metadatos de sincronización (migración 10→11, aditivos y conservadores) ---
    val syncState: String = "LOCAL_ONLY",
    val syncError: String? = null,
    val updatedAt: Long = 0
) {
    val isOpen: Boolean get() = !isClosed

    val periodLabel: String get() = "%02d/%04d".format(java.util.Locale.ROOT, month, year)

    val assignedStudentsSet: Set<String>
        get() = assignedStudentEmails
            .split(',')
            .map { it.trim().lowercase() }
            .filter(String::isNotBlank)
            .toSet()

    /** Determina si un estudiante tiene acceso por asignación directa o por pertenecer al curso. */
    fun isAccessibleBy(student: UserAccount): Boolean {
        if (student.role != cl.duoc.coegio_pablo_saladana.domain.model.UserRole.Student) return false
        val normalizedStudentEmail = student.email.trim().lowercase()
        val assigned = assignedStudentsSet
        val normalizedCourse = course.trim().ifBlank { SchoolCourses.GENERAL }
        return normalizedCourse in SchoolCourses.STUDENT_COURSES &&
            student.course.trim().equals(normalizedCourse, ignoreCase = true) &&
            (assigned.isEmpty() || normalizedStudentEmail in assigned)
    }
}
