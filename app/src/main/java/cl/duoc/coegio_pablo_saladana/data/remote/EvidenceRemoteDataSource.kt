package cl.duoc.coegio_pablo_saladana.data.remote

data class DownloadedEvidence(val bytes: ByteArray, val mimeType: String, val fileName: String)

object EvidenceRemoteDataSource {
    var lastError: String? = null
        private set

    suspend fun download(id: Long): DownloadedEvidence? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        return runCatching {
            val response = SchoolBackend.api.evidence(id)
            if (!response.isSuccessful) error("HTTP ${response.code()}")
            val body = requireNotNull(response.body())
            val header = response.headers()["Content-Disposition"].orEmpty()
            val name = Regex("filename=\"?([^\";]+)").find(header)?.groupValues?.get(1)
                ?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: "evidence-$id"
            DownloadedEvidence(body.bytes(), body.contentType()?.toString() ?: "application/octet-stream", name)
        }.onFailure { lastError = backendMessage("evidencias", it) }.getOrNull()
    }
}
