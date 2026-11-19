package cl.duoc.coegio_pablo_saladana.data.remote.dto

/** Tipo de evidencia. Gson puede dejarlo null si el servidor aún no lo envía. */
enum class EvidenceKind { PHOTO, FILE, LINK }

/**
 * Estado de transferencia de una evidencia.
 * - PENDING: archivo local (content://) que aún no se ha subido; nunca es descargable por otros.
 * - UPLOADED: el backend ya entregó una referencia remota (remoteRef).
 * - FAILED: el último intento de subida falló.
 * - NOT_APPLICABLE: enlaces http/https, ya son compartibles tal cual.
 */
enum class EvidenceTransferState { PENDING, UPLOADED, FAILED, NOT_APPLICABLE }

/**
 * DTO de evidencia para el contrato REST propuesto (aún NO implementado por el backend).
 *
 * Regla de oro: una URI `content://` de Android jamás viaja en [url] ni en [remoteRef];
 * solo vive en el almacenamiento local (Room). Aquí solo se intercambian metadatos y
 * referencias remotas reales (https://... o un identificador del servidor).
 */
data class EvidenceDto(
    val kind: EvidenceKind? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    /** Referencia entregada por el backend tras la subida; null mientras esté pendiente. */
    val remoteRef: String? = null,
    /** Solo para enlaces http/https compartibles; nunca content://. */
    val url: String? = null,
    val transferState: EvidenceTransferState? = null
) {
    /** ¿Puede otro dispositivo abrir esta evidencia hoy? */
    val isShareable: Boolean
        get() = when (kind) {
            EvidenceKind.LINK -> url?.startsWith("http://") == true || url?.startsWith("https://") == true
            EvidenceKind.PHOTO, EvidenceKind.FILE ->
                transferState == EvidenceTransferState.UPLOADED && !remoteRef.isNullOrBlank()
            null -> false
        }
}
