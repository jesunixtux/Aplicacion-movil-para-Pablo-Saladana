package cl.duoc.coegio_pablo_saladana.data.remote.dto

/**
 * Traduce entre las evidencias guardadas localmente (líneas de texto en Room) y los
 * DTO del contrato propuesto.
 *
 * Reglas de seguridad:
 * - Una URI `content://` NUNCA se convierte en url ni remoteRef: queda PENDING y sin
 *   referencia compartible hasta que una subida real entregue un identificador remoto.
 * - Solo los enlaces http/https se consideran compartibles (NOT_APPLICABLE).
 * - Los metadatos (nombre, tipo, tamaño) se completarán al subir, leyendo el
 *   ContentResolver con los permisos persistidos; aquí no se inventan.
 */
object EvidenceMappers {
    fun fromLocalLines(photoUris: String, fileUris: String, links: String): List<EvidenceDto> {
        val photos = photoUris.lineSequence().filter(String::isNotBlank).map { uri ->
            localFileEvidence(uri.trim(), EvidenceKind.PHOTO)
        }
        val files = fileUris.lineSequence().filter(String::isNotBlank).map { uri ->
            localFileEvidence(uri.trim(), EvidenceKind.FILE)
        }
        val urls = links.lineSequence().filter(String::isNotBlank).mapNotNull { line ->
            val url = line.trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                EvidenceDto(kind = EvidenceKind.LINK, url = url, transferState = EvidenceTransferState.NOT_APPLICABLE)
            } else {
                null
            }
        }
        return (photos + files + urls).toList()
    }

    private fun localFileEvidence(uri: String, kind: EvidenceKind): EvidenceDto =
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // Referencia remota ya subida en otro dispositivo: es compartible.
            EvidenceDto(kind = kind, remoteRef = uri, transferState = EvidenceTransferState.UPLOADED)
        } else {
            // content:// u otra URI local: pendiente de transferencia, nunca compartible.
            EvidenceDto(kind = kind, transferState = EvidenceTransferState.PENDING)
        }

    /** Solo referencias remotas reales vuelven a las líneas locales de fotos. */
    fun toLocalPhotoLines(evidences: List<EvidenceDto>?): String =
        remoteLines(evidences, EvidenceKind.PHOTO)

    fun toLocalFileLines(evidences: List<EvidenceDto>?): String =
        remoteLines(evidences, EvidenceKind.FILE)

    private fun remoteLines(evidences: List<EvidenceDto>?, kind: EvidenceKind): String =
        evidences.orEmpty()
            .filter { it.kind == kind && it.isShareable && !it.remoteRef.isNullOrBlank() }
            .mapNotNull { it.remoteRef }
            .joinToString("\n")

    fun toLocalLinkLines(evidences: List<EvidenceDto>?): String =
        evidences.orEmpty()
            .filter { it.kind == EvidenceKind.LINK && it.isShareable }
            .mapNotNull { it.url }
            .joinToString("\n")
}
