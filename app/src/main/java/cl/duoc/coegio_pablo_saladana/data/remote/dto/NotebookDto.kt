package cl.duoc.coegio_pablo_saladana.data.remote.dto

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects

/**
 * DTO completo de bitácora para el contrato REST propuesto.
 *
 * Los campos marcados como "propuesto" todavía NO existen en el backend actual
 * (pablo-salandra-backend solo envía hasta assignedStudentEmails). Se declaran
 * nullables para que las respuestas legacy del backend se deserialicen sin fallo
 * y la app conserve los valores por defecto de Room.
 */
data class NotebookDto(
    val id: Long = 0,
    val title: String? = null,
    val purpose: String? = null,
    val instructions: String? = null,
    val questions: String? = null,
    val teacherEmail: String? = null,
    val teacherName: String? = null,
    val createdAt: Long = 0,
    val course: String? = null,
    val assignedStudentEmails: String? = null,
    // --- Campos propuestos: pendientes de implementar en Spring Boot/MySQL ---
    val subject: String? = null,
    val month: Int? = null,
    val year: Int? = null,
    val publishedAt: Long? = null,
    val isClosed: Boolean? = null,
    val closedAt: Long? = null,
    val remindersEnabled: Boolean? = null,
    /** Propuesto: marca de última modificación para resolver conflictos de sincronización. */
    val updatedAt: Long? = null
) {
    /**
     * Convierte a entidad Room conservando compatibilidad legacy: sin título no hay
     * bitácora; los campos ausentes usan los mismos defaults que NotebookEntity.
     * [localId] permite conservar el identificador local al actualizar una fila existente.
     */
    fun toEntity(
        localId: Long = 0,
        remoteOwnerId: Long? = null,
        cachedForUserId: Long? = null
    ): NotebookEntity? {
        if (title.isNullOrBlank()) return null
        return NotebookEntity(
            id = localId,
            title = title,
            purpose = purpose.orEmpty(),
            instructions = instructions.orEmpty(),
            questions = questions.orEmpty(),
            teacherEmail = teacherEmail.orEmpty(),
            teacherName = teacherName.orEmpty(),
            createdAt = createdAt,
            course = course?.takeIf(String::isNotBlank) ?: SchoolCourses.GENERAL,
            assignedStudentEmails = assignedStudentEmails.orEmpty(),
            remoteId = id.takeIf { it > 0 },
            subject = subject?.takeIf(String::isNotBlank) ?: SchoolSubjects.DEFAULT,
            month = month ?: 1,
            year = year ?: 2026,
            isClosed = isClosed ?: false,
            publishedAt = publishedAt ?: 0,
            closedAt = closedAt,
            remindersEnabled = remindersEnabled ?: true,
            updatedAt = updatedAt ?: createdAt,
            remoteOwnerId = remoteOwnerId,
            cachedForUserId = cachedForUserId
        )
    }

    companion object {
        /** Vuelca la entidad local completa al DTO, sin perder ningún campo del MVP. */
        fun fromEntity(entity: NotebookEntity): NotebookDto = NotebookDto(
            id = entity.remoteId ?: 0,
            title = entity.title,
            purpose = entity.purpose,
            instructions = entity.instructions,
            questions = entity.questions,
            teacherEmail = entity.teacherEmail,
            teacherName = entity.teacherName,
            createdAt = entity.createdAt,
            course = entity.course,
            assignedStudentEmails = entity.assignedStudentEmails,
            subject = entity.subject,
            month = entity.month,
            year = entity.year,
            publishedAt = entity.publishedAt,
            isClosed = entity.isClosed,
            closedAt = entity.closedAt,
            remindersEnabled = entity.remindersEnabled,
            updatedAt = entity.updatedAt
        )
    }
}
