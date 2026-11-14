package cl.duoc.coegio_pablo_saladana.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
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
    val remoteId: Long? = null
) {
    val assignedStudentsSet: Set<String>
        get() = assignedStudentEmails
            .split(',')
            .map { it.trim().lowercase() }
            .filter(String::isNotBlank)
            .toSet()

    /** Determina si un estudiante tiene acceso por asignación directa o por pertenecer al curso. */
    fun isAccessibleBy(student: UserAccount): Boolean {
        val normalizedStudentEmail = student.email.trim().lowercase()
        val assigned = assignedStudentsSet
        if (normalizedStudentEmail in assigned) return true
        val normalizedCourse = course.trim().ifBlank { SchoolCourses.GENERAL }
        if (normalizedCourse.equals(SchoolCourses.CUSTOM, ignoreCase = true)) {
            return false
        }
        if (normalizedCourse.equals(SchoolCourses.GENERAL, ignoreCase = true)) {
            return assigned.isEmpty()
        }
        return student.course.trim().equals(normalizedCourse, ignoreCase = true)
    }
}
