package cl.duoc.coegio_pablo_saladana.data.remote

import cl.duoc.coegio_pablo_saladana.BuildConfig
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.remote.auth.AuthResponseDto
import cl.duoc.coegio_pablo_saladana.data.remote.auth.RemoteSessionStore
import cl.duoc.coegio_pablo_saladana.data.remote.auth.RemoteUserDto
import cl.duoc.coegio_pablo_saladana.data.remote.auth.SessionTokenInterceptor
import cl.duoc.coegio_pablo_saladana.data.remote.auth.SessionTokenProvider
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceDto
import cl.duoc.coegio_pablo_saladana.data.remote.dto.NotebookDto
import cl.duoc.coegio_pablo_saladana.data.remote.dto.StudentEntryDto
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

internal data class LoginInput(val email: String, val password: String)
internal data class RegisterInput(val name: String, val email: String, val password: String, val course: String)

internal data class NotebookInput(
    val title: String,
    val purpose: String,
    val instructions: String,
    val questions: String,
    val course: String,
    val assignedStudentEmails: String,
    val subject: String,
    val month: Int,
    val year: Int
)

internal data class StudentEntryInput(
    val notebookId: Long,
    val notebookTitle: String,
    val classDate: String,
    val content: String,
    val activity: String,
    val learning: String,
    val doubts: String,
    val isDraft: Boolean,
    val isReviewed: Boolean = false,
    val teacherFeedback: String = ""
)

internal data class ReviewInput(val feedback: String)
internal data class RemindersInput(val enabled: Boolean)

internal interface SchoolApi {
    @POST("api/auth/login") suspend fun login(@Body input: LoginInput): AuthResponseDto
    @POST("api/auth/register") suspend fun register(@Body input: RegisterInput): AuthResponseDto
    @GET("api/auth/me") suspend fun me(): RemoteUserDto
    @POST("api/auth/logout") suspend fun logout(): Response<Unit>

    @GET("api/notebooks") suspend fun notebooks(): List<NotebookDto>
    @POST("api/notebooks") suspend fun createNotebook(
        @Header("Idempotency-Key") key: String, @Body input: NotebookInput
    ): NotebookDto
    @PUT("api/notebooks/{id}") suspend fun updateNotebook(
        @Path("id") id: Long, @Header("If-Match") version: String, @Body input: NotebookInput
    ): NotebookDto
    @POST("api/notebooks/{id}/publish") suspend fun publishNotebook(
        @Path("id") id: Long, @Header("If-Match") version: String
    ): NotebookDto
    @POST("api/notebooks/{id}/close") suspend fun closeNotebook(
        @Path("id") id: Long, @Header("If-Match") version: String
    ): NotebookDto
    @PUT("api/notebooks/{id}/reminders") suspend fun reminders(
        @Path("id") id: Long, @Header("If-Match") version: String, @Body input: RemindersInput
    ): NotebookDto

    @GET("api/student-entries") suspend fun entries(
        @Query("studentEmail") studentEmail: String? = null,
        @Query("teacherEmail") teacherEmail: String? = null
    ): List<StudentEntryDto>
    @GET("api/student-entries/{id}") suspend fun entry(@Path("id") id: Long): StudentEntryDto
    @POST("api/student-entries") suspend fun createEntry(
        @Header("Idempotency-Key") key: String, @Body input: StudentEntryInput
    ): StudentEntryDto
    @PUT("api/student-entries/{id}") suspend fun updateEntry(
        @Path("id") id: Long, @Header("If-Match") version: String, @Body input: StudentEntryInput
    ): StudentEntryDto
    @POST("api/student-entries/{id}/submit") suspend fun submitEntry(
        @Path("id") id: Long, @Header("If-Match") version: String
    ): StudentEntryDto
    @PUT("api/student-entries/{id}/review") suspend fun reviewEntry(
        @Path("id") id: Long, @Header("If-Match") version: String, @Body input: ReviewInput
    ): StudentEntryDto

    @Multipart
    @POST("api/evidences")
    suspend fun uploadEvidence(
        @Header("Idempotency-Key") key: String,
        @Part("entryRemoteId") entryRemoteId: RequestBody,
        @Part("kind") kind: RequestBody,
        @Part file: MultipartBody.Part? = null,
        @Part("url") url: RequestBody? = null
    ): EvidenceDto

    @Streaming
    @GET("api/evidences/{id}")
    suspend fun evidence(@Path("id") id: Long): Response<ResponseBody>
}

internal object SchoolBackend {
    @Volatile private var sessionStore: RemoteSessionStore? = null

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(SessionTokenInterceptor(SessionTokenProvider { sessionStore?.currentToken() }) {
            sessionStore?.clear()
        })
        .build()

    val remoteEnabled: Boolean = BuildConfig.REMOTE_SYNC_ENABLED && safeBaseUrl(BuildConfig.API_BASE_URL)
    val api: SchoolApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SchoolApi::class.java)

    fun configure(store: RemoteSessionStore) { sessionStore = store }
    fun hasSession(): Boolean = sessionStore?.currentToken() != null

    private fun safeBaseUrl(value: String): Boolean {
        val url = runCatching { java.net.URI(value) }.getOrNull() ?: return false
        if (url.scheme.equals("https", true)) return true
        if (!url.scheme.equals("http", true)) return false
        val host = url.host ?: return false
        return host == "10.0.2.2" || host == "127.0.0.1" || host == "localhost" ||
            host.startsWith("192.168.") || host.startsWith("10.") || host.endsWith(".local")
    }
}

internal fun backendMessage(area: String, error: Throwable): String {
    val status = (error as? HttpException)?.code()
    val detail = when (status) {
        401 -> "La sesión expiró. Inicia sesión nuevamente."
        403 -> "Tu cuenta no tiene permiso para esta operación."
        404 -> "El recurso no existe o no está autorizado para tu cuenta."
        409 -> "El servidor rechazó una operación duplicada o incompatible."
        412 -> "Los datos cambiaron en otro dispositivo; vuelve a sincronizar."
        else -> AppErrors.describe(error)
    }
    return "Error del backend de $area (${BuildConfig.API_BASE_URL}). $detail"
}

object NotebookRemoteDataSource {
    private val gson = Gson()
    var lastError: String? = null
        private set

    suspend fun fetchAll(): List<NotebookDto>? = call { SchoolBackend.api.notebooks() }

    suspend fun create(notebook: NotebookEntity): NotebookDto? = call {
        SchoolBackend.api.createNotebook(
            "android-notebook-${notebook.remoteOwnerId}-${notebook.createdAt}-${notebook.id}", input(notebook)
        )
    }

    suspend fun update(notebook: NotebookEntity, remoteVersion: Long): NotebookDto? {
        val id = notebook.remoteId ?: return fail("La bitácora no tiene identificador remoto.")
        return call { SchoolBackend.api.updateNotebook(id, remoteVersion.toString(), input(notebook)) }
    }

    suspend fun publish(remoteId: Long, version: Long): NotebookDto? =
        call { SchoolBackend.api.publishNotebook(remoteId, version.toString()) }

    suspend fun close(remoteId: Long, version: Long): NotebookDto? =
        call { SchoolBackend.api.closeNotebook(remoteId, version.toString()) }

    suspend fun reminders(remoteId: Long, version: Long, enabled: Boolean): NotebookDto? =
        call { SchoolBackend.api.reminders(remoteId, version.toString(), RemindersInput(enabled)) }

    private suspend fun <T> call(block: suspend () -> T): T? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) return fail(AppErrors.REMOTE_DISABLED)
        return runCatching { block() }.onFailure { lastError = backendMessage("bitácoras", it) }.getOrNull()
    }

    private fun <T> fail(message: String): T? { lastError = message; return null }

    private fun input(notebook: NotebookEntity) = NotebookInput(
        notebook.title, notebook.purpose, notebook.instructions, notebook.questions,
        notebook.course, notebook.assignedStudentEmails, notebook.subject, notebook.month, notebook.year
    )

    internal fun encodeNotebookInput(draft: NotebookDraft, teacherEmail: String, teacherName: String): String {
        val normalized = draft.normalized()
        return gson.toJson(NotebookInput(
            normalized.title, normalized.purpose, normalized.instructions, normalized.questions,
            normalized.course, normalized.assignedStudentEmails.sorted().joinToString(","),
            normalized.subject, normalized.month, normalized.year
        ))
    }

    internal fun parseNotebookList(json: String): List<NotebookEntity> = runCatching {
        val type = object : TypeToken<List<NotebookDto>>() {}.type
        (gson.fromJson<List<NotebookDto>>(json, type) ?: emptyList()).mapNotNull { it.toEntity() }
    }.getOrDefault(emptyList())

    internal fun parseNotebookObject(json: String): NotebookEntity? = runCatching {
        gson.fromJson(json, NotebookDto::class.java)?.toEntity()
    }.getOrNull()
}
