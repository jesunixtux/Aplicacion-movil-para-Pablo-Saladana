package cl.duoc.coegio_pablo_saladana.data

import android.util.JsonReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cliente REST para sincronizar bitácoras con el microservicio Spring Boot. */
object NotebookRemoteDataSource {
    private const val BASE_URL = "http://10.0.2.2:8080/api/notebooks"
    private const val TIMEOUT_MS = 2_500

    suspend fun fetchAll(): List<NotebookEntity>? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(BASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseNotebookList(body)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = encodeNotebookInput(draft, teacher.email, teacher.fullName)
                val connection = (URL(BASE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    connection.outputStream.bufferedWriter().use { it.write(payload) }
                    if (connection.responseCode !in 200..299) return@runCatching null
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseNotebookObject(body)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    internal fun encodeNotebookInput(
        draft: NotebookDraft,
        teacherEmail: String,
        teacherName: String
    ): String {
        val normalized = draft.normalized()
        return buildString {
            append('{')
            append("\"title\":\"").append(escapeJson(normalized.title)).append("\",")
            append("\"purpose\":\"").append(escapeJson(normalized.purpose)).append("\",")
            append("\"instructions\":\"").append(escapeJson(normalized.instructions)).append("\",")
            append("\"questions\":\"").append(escapeJson(normalized.questions)).append("\",")
            append("\"teacherEmail\":\"").append(escapeJson(teacherEmail.trim())).append("\",")
            append("\"teacherName\":\"").append(escapeJson(teacherName.trim())).append('"')
            append('}')
        }
    }

    internal fun parseNotebookList(json: String): List<NotebookEntity> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        return extractJsonObjects(trimmed).mapNotNull(::parseNotebookObject)
    }

    internal fun parseNotebookObject(json: String): NotebookEntity? {
        val fields = parseFlatJsonMap(json)
        val title = fields["title"] ?: return null
        return NotebookEntity(
            id = fields["id"]?.toLongOrNull() ?: 0L,
            title = title,
            purpose = fields["purpose"].orEmpty(),
            instructions = fields["instructions"].orEmpty(),
            questions = fields["questions"].orEmpty(),
            teacherEmail = fields["teacherEmail"].orEmpty(),
            teacherName = fields["teacherName"].orEmpty(),
            createdAt = fields["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }

    internal fun extractJsonObjects(jsonArray: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inQuotes = false
        var escaped = false
        for (i in jsonArray.indices) {
            val ch = jsonArray[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && inQuotes) {
                escaped = true
                continue
            }
            if (ch == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (ch == '{') {
                    if (depth == 0) start = i
                    depth++
                } else if (ch == '}') {
                    depth--
                    if (depth == 0 && start >= 0) {
                        objects.add(jsonArray.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return objects
    }

    internal fun parseFlatJsonMap(jsonObject: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pairRegex = Regex("\"([^\"]+)\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|([^,}]+))")
        for (match in pairRegex.findAll(jsonObject)) {
            val key = match.groupValues[1]
            val rawValue = if (match.groups[2] != null) {
                unescapeJson(match.groupValues[2])
            } else {
                match.groupValues[3].trim()
            }
            if (rawValue != "null") {
                result[key] = rawValue
            }
        }
        return result
    }

    private fun escapeJson(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    private fun unescapeJson(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}
