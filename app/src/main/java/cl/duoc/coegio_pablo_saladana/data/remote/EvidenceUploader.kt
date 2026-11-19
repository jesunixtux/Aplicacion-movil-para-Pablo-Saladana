package cl.duoc.coegio_pablo_saladana.data.remote

import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceDto
import java.io.InputStream
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** Resultado de un intento de subida de evidencia. */
sealed interface EvidenceUploadResult {
    /** El backend aceptó el archivo y entregó una referencia remota real. */
    data class Uploaded(val remoteRef: String) : EvidenceUploadResult

    /** Fallo explícito (red, permisos, tamaño o integración desactivada). */
    data class Failed(val message: String) : EvidenceUploadResult
}

/**
 * Abstracción para subir evidencias al backend (Tarea 4).
 *
 * El llamador abre el stream desde el ContentResolver usando los permisos de lectura
 * persistidos al seleccionar el archivo; la implementación lo envía como multipart.
 * Nunca se envía la URI content:// como dirección: solo los bytes y sus metadatos.
 */
interface EvidenceUploader {
    suspend fun upload(evidence: EvidenceDto, openStream: () -> InputStream): EvidenceUploadResult
}

/**
 * Implementación activa mientras la integración está desactivada: no abre streams,
 * no inventa URLs y reporta el motivo exacto. La subida real se conectará cuando el
 * backend implemente el contrato propuesto.
 */
class DisabledEvidenceUploader : EvidenceUploader {
    override suspend fun upload(evidence: EvidenceDto, openStream: () -> InputStream): EvidenceUploadResult =
        EvidenceUploadResult.Failed(
            "La subida de evidencias está desactivada: el backend aún no implementa el endpoint de archivos. " +
                "La evidencia local se conserva en el dispositivo."
        )
}

/**
 * CONTRATO PROPUESTO — NO implementado todavía por el backend y SIN usar en producción.
 * Se documenta aquí para que Spring Boot lo implemente tal cual (ver
 * PREPARACION-MIGRACION-BACKEND.md, sección de evidencias). No se instancia en ningún lugar.
 */
internal interface ProposedEvidenceUploadApi {
    @Multipart
    @POST("api/evidences")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("entryRemoteId") entryRemoteId: Long,
        @Part("kind") kind: String
    ): EvidenceDto
}
