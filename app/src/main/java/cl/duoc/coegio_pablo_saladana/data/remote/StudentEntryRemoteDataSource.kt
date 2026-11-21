package cl.duoc.coegio_pablo_saladana.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.dto.StudentEntryDto
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import java.security.MessageDigest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

internal typealias StudentEntryPayload = StudentEntryDto

internal object StudentEntryRemoteDataSource {
    var lastError: String? = null
        private set

    suspend fun fetchByStudent(email: String): List<StudentEntryDto>? =
        call { SchoolBackend.api.entries(studentEmail = email) }

    suspend fun fetchByTeacher(email: String): List<StudentEntryDto>? =
        call { SchoolBackend.api.entries(teacherEmail = email) }

    suspend fun create(entry: StudentEntryEntity, notebook: NotebookEntity): StudentEntryDto? {
        val notebookId = notebook.remoteId ?: return fail("La bitácora todavía no existe en el servidor.")
        return call {
            SchoolBackend.api.createEntry(
                "android-entry-${entry.remoteOwnerId}-${entry.createdAt}-${entry.id}",
                input(entry, notebookId, forceDraft = true)
            )
        }
    }

    suspend fun update(entry: StudentEntryEntity, notebook: NotebookEntity, remoteVersion: Long): StudentEntryDto? {
        val entryId = entry.remoteId ?: return create(entry, notebook)
        val notebookId = notebook.remoteId ?: return fail("La bitácora todavía no existe en el servidor.")
        return call {
            SchoolBackend.api.updateEntry(entryId, remoteVersion.toString(), input(entry, notebookId, forceDraft = true))
        }
    }

    suspend fun submit(remoteId: Long, version: Long): StudentEntryDto? =
        call { SchoolBackend.api.submitEntry(remoteId, version.toString()) }

    suspend fun review(remoteId: Long, version: Long, feedback: String): StudentEntryDto? =
        call { SchoolBackend.api.reviewEntry(remoteId, version.toString(), ReviewInput(feedback.trim())) }

    /** Sube bytes reales; una URI content:// nunca se envía como URL. */
    suspend fun uploadEvidences(context: Context, entry: StudentEntryEntity, remoteId: Long): StudentEntryDto? {
        val resolver = context.contentResolver
        val uploads = buildList {
            entry.photoUris.lineSequence().filter { it.startsWith("content://") }
                .forEach { add("PHOTO" to it) }
            entry.fileUris.lineSequence().filter { it.startsWith("content://") }
                .forEach { add("FILE" to it) }
        }
        for ((kind, rawUri) in uploads) {
            val uri = Uri.parse(rawUri)
            val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                ?: return fail("No se pudo leer una evidencia local; el registro se conserva como borrador.")
            if (bytes.isEmpty() || bytes.size > 10 * 1024 * 1024)
                return fail("Una evidencia está vacía o supera el límite de 10 MB.")
            val mime = resolver.getType(uri)
                ?: return fail("No se pudo determinar el tipo de una evidencia local.")
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "evidence"
            val body = bytes.toRequestBody(mime.toMediaType())
            val part = MultipartBody.Part.createFormData("file", name, body)
            val key = "android-evidence-$remoteId-$kind-${sha256(rawUri)}"
            if (call {
                    SchoolBackend.api.uploadEvidence(
                        key,
                        remoteId.toString().toRequestBody("text/plain".toMediaType()),
                        kind.toRequestBody("text/plain".toMediaType()),
                        part
                    )
                } == null) return null
        }
        entry.links.lineSequence().map(String::trim).filter { it.startsWith("http://") || it.startsWith("https://") }
            .forEach { url ->
                val key = "android-evidence-$remoteId-LINK-${sha256(url)}"
                if (call {
                        SchoolBackend.api.uploadEvidence(
                            key,
                            remoteId.toString().toRequestBody("text/plain".toMediaType()),
                            "LINK".toRequestBody("text/plain".toMediaType()),
                            url = url.toRequestBody("text/plain".toMediaType())
                        )
                    } == null) return null
            }
        return call { SchoolBackend.api.entry(remoteId) }
    }

    private fun input(entry: StudentEntryEntity, notebookId: Long, forceDraft: Boolean) = StudentEntryInput(
        notebookId = notebookId,
        notebookTitle = entry.notebookTitle,
        classDate = entry.classDate,
        content = entry.content,
        activity = entry.activity,
        learning = entry.learning,
        doubts = entry.doubts,
        isDraft = forceDraft
    )

    private suspend fun <T> call(block: suspend () -> T): T? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) return fail(AppErrors.REMOTE_DISABLED)
        return runCatching { block() }.onFailure { lastError = backendMessage("registros", it) }.getOrNull()
    }

    private fun <T> fail(message: String): T? { lastError = message; return null }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
