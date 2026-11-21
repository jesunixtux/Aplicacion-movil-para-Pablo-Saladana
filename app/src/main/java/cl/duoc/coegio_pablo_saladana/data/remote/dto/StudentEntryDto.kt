package cl.duoc.coegio_pablo_saladana.data.remote.dto

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity

/**
 * DTO completo de registro de clase para el contrato REST propuesto.
 *
 * El backend actual (pablo-salandra-backend) envía hasta teacherFeedback; los campos
 * propuestos (submittedAt, reviewedAt, reviewerEmail, evidences) son nullables para
 * aceptar respuestas legacy sin romper la deserialización.
 */
data class StudentEntryDto(
    val id: Long = 0,
    /** Identificador REMOTO (MySQL) de la bitácora, no el id local de Room. */
    val notebookId: Long = 0,
    val notebookTitle: String? = null,
    val teacherEmail: String? = null,
    val studentEmail: String? = null,
    val studentName: String? = null,
    val classDate: String? = null,
    val content: String? = null,
    val activity: String? = null,
    val learning: String? = null,
    val doubts: String? = null,
    val isDraft: Boolean = false,
    val createdAt: Long = 0,
    val isReviewed: Boolean = false,
    val teacherFeedback: String? = null,
    // --- Campos propuestos: pendientes de implementar en Spring Boot/MySQL ---
    val submittedAt: Long? = null,
    val reviewedAt: Long? = null,
    val reviewerEmail: String? = null,
    val evidences: List<EvidenceDto>? = null,
    /** Propuesto: marca de última modificación para resolver conflictos. */
    val updatedAt: Long? = null
) {
    /**
     * Convierte a entidad Room. [notebook] aporta el id LOCAL y el título de respaldo,
     * igual que el payload legacy. Sin submittedAt remoto, un registro enviado hereda
     * createdAt como fecha de envío (misma regla que la migración Room 9→10).
     */
    fun toEntity(notebook: NotebookEntity, remoteOwnerId: Long? = null): StudentEntryEntity = StudentEntryEntity(
        notebookId = notebook.id,
        notebookTitle = notebookTitle ?: notebook.title,
        studentEmail = studentEmail.orEmpty(),
        studentName = studentName.orEmpty(),
        classDate = classDate.orEmpty(),
        content = content.orEmpty(),
        activity = activity.orEmpty(),
        learning = learning.orEmpty(),
        doubts = doubts.orEmpty(),
        isDraft = isDraft,
        createdAt = createdAt,
        isReviewed = isReviewed,
        teacherFeedback = teacherFeedback.orEmpty(),
        remoteId = id.takeIf { it > 0 },
        photoUris = EvidenceMappers.toLocalPhotoLines(evidences),
        fileUris = EvidenceMappers.toLocalFileLines(evidences),
        links = EvidenceMappers.toLocalLinkLines(evidences),
        submittedAt = submittedAt ?: createdAt.takeIf { !isDraft },
        reviewedAt = reviewedAt,
        reviewerEmail = reviewerEmail.orEmpty(),
        updatedAt = updatedAt ?: createdAt,
        remoteOwnerId = remoteOwnerId
    )

    companion object {
        /**
         * Vuelca la entidad local al DTO. [remoteNotebookId] debe ser el id de MySQL
         * de la bitácora; si la bitácora aún no tiene id remoto, el registro no puede
         * enviarse todavía (queda pendiente de sincronización).
         */
        fun fromEntity(entry: StudentEntryEntity, remoteNotebookId: Long): StudentEntryDto = StudentEntryDto(
            id = entry.remoteId ?: 0,
            notebookId = remoteNotebookId,
            notebookTitle = entry.notebookTitle,
            studentEmail = entry.studentEmail,
            studentName = entry.studentName,
            classDate = entry.classDate,
            content = entry.content,
            activity = entry.activity,
            learning = entry.learning,
            doubts = entry.doubts,
            isDraft = entry.isDraft,
            createdAt = entry.createdAt,
            isReviewed = entry.isReviewed,
            teacherFeedback = entry.teacherFeedback,
            submittedAt = entry.submittedAt,
            reviewedAt = entry.reviewedAt,
            reviewerEmail = entry.reviewerEmail,
            evidences = EvidenceMappers.fromLocalLines(entry.photoUris, entry.fileUris, entry.links),
            updatedAt = entry.updatedAt
        )
    }
}
