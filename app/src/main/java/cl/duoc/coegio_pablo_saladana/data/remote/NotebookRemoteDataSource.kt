package cl.duoc.coegio_pablo_saladana.data.remote

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal data class NotebookPayload(
    val id: Long = 0,
    val title: String? = null,
    val purpose: String? = null,
    val instructions: String? = null,
    val questions: String? = null,
    val teacherEmail: String? = null,
    val teacherName: String? = null,
    val createdAt: Long = 0,
    val course: String? = null,
    val assignedStudentEmails: String? = null
) {
    fun toEntity(): NotebookEntity? {
        if (title.isNullOrBlank()) return null
        return NotebookEntity(
            title = title,
            purpose = purpose.orEmpty(),
            instructions = instructions.orEmpty(),
            questions = questions.orEmpty(),
            teacherEmail = teacherEmail.orEmpty(),
            teacherName = teacherName.orEmpty(),
            createdAt = createdAt,
            course = course?.takeIf(String::isNotBlank) ?: SchoolCourses.GENERAL,
            assignedStudentEmails = assignedStudentEmails.orEmpty(),
            remoteId = id.takeIf { it > 0 }
        )
    }
}

internal data class NotebookInput(
    val title: String,
    val purpose: String,
    val instructions: String,
    val questions: String,
    val teacherEmail: String,
    val teacherName: String,
    val course: String,
    val assignedStudentEmails: String
)

internal interface SchoolApi {
    @GET("api/notebooks")
    suspend fun notebooks(): List<NotebookPayload>

    @POST("api/notebooks")
    suspend fun createNotebook(@Body input: NotebookInput): NotebookPayload

    @PUT("api/notebooks/{id}")
    suspend fun updateNotebook(@Path("id") id: Long, @Body input: NotebookInput): NotebookPayload

    @GET("api/student-entries")
    suspend fun entries(
        @Query("studentEmail") studentEmail: String? = null,
        @Query("teacherEmail") teacherEmail: String? = null
    ): List<StudentEntryPayload>

    @POST("api/student-entries")
    suspend fun createEntry(@Body input: StudentEntryInput): StudentEntryPayload

    @PUT("api/student-entries/{id}")
    suspend fun updateEntry(@Path("id") id: Long, @Body input: StudentEntryInput): StudentEntryPayload
}

/**
 * Cliente del backend. La URL sale de BuildConfig (Tarea 5):
 * emulador por defecto, dispositivo físico o servidor externo vía -PapiBaseUrl.
 * La comunicación remota permanece desactivada (BuildConfig.REMOTE_SYNC_ENABLED=false).
 */
internal object SchoolBackend {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        // Sin sesión remota todavía: el proveedor por defecto no entrega token y no se adjunta nada.
        .addInterceptor(
            cl.duoc.coegio_pablo_saladana.data.remote.auth.SessionTokenInterceptor(
                cl.duoc.coegio_pablo_saladana.data.remote.auth.SessionTokenProvider { null }
            )
        )
        .build()

    /** Puerta única de activación remota; nadie llama al backend si está en false. */
    val remoteEnabled: Boolean = cl.duoc.coegio_pablo_saladana.BuildConfig.REMOTE_SYNC_ENABLED

    val api: SchoolApi = Retrofit.Builder()
        .baseUrl(cl.duoc.coegio_pablo_saladana.BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SchoolApi::class.java)
}

/** Sincroniza bitácoras con Spring Boot y conserva el identificador remoto. */
object NotebookRemoteDataSource {
    private val gson = Gson()

    /** Motivo del último fallo con el backend; null si la última llamada fue exitosa. */
    var lastError: String? = null
        private set

    suspend fun fetchAll(): List<NotebookEntity>? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        return runCatching {
            SchoolBackend.api.notebooks().mapNotNull(NotebookPayload::toEntity)
        }.onFailure { lastError = backendMessage(it) }.getOrNull()
    }

    suspend fun create(draft: NotebookDraft, teacher: UserAccount): NotebookEntity? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        return runCatching {
            SchoolBackend.api.createNotebook(input(draft, teacher.email, teacher.fullName)).toEntity()
        }.onFailure { lastError = backendMessage(it) }.getOrNull()
    }

    suspend fun update(notebook: NotebookEntity): NotebookEntity? {
        lastError = null
        if (!SchoolBackend.remoteEnabled) {
            lastError = cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED
            return null
        }
        val remoteId = notebook.remoteId ?: run {
            lastError = "La bitácora no tiene identificador remoto; no se puede actualizar en el servidor."
            return null
        }
        return runCatching {
            SchoolBackend.api.updateNotebook(remoteId, input(notebook)).toEntity()
        }.onFailure { lastError = backendMessage(it) }.getOrNull()
    }

    private fun backendMessage(error: Throwable): String =
        "Error del backend de bitácoras (${cl.duoc.coegio_pablo_saladana.BuildConfig.API_BASE_URL}). " +
            cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.describe(error)

    private fun input(draft: NotebookDraft, teacherEmail: String, teacherName: String): NotebookInput {
        val normalized = draft.normalized()
        return NotebookInput(
            normalized.title, normalized.purpose, normalized.instructions, normalized.questions,
            teacherEmail.trim(), teacherName.trim(), normalized.course,
            normalized.assignedStudentEmails.sorted().joinToString(",")
        )
    }

    private fun input(notebook: NotebookEntity) = NotebookInput(
        notebook.title, notebook.purpose, notebook.instructions, notebook.questions,
        notebook.teacherEmail, notebook.teacherName, notebook.course, notebook.assignedStudentEmails
    )

    internal fun encodeNotebookInput(draft: NotebookDraft, teacherEmail: String, teacherName: String): String =
        gson.toJson(input(draft, teacherEmail, teacherName))

    internal fun parseNotebookList(json: String): List<NotebookEntity> = runCatching {
        val type = object : TypeToken<List<NotebookPayload>>() {}.type
        (gson.fromJson<List<NotebookPayload>>(json, type) ?: emptyList()).mapNotNull(NotebookPayload::toEntity)
    }.getOrDefault(emptyList())

    internal fun parseNotebookObject(json: String): NotebookEntity? = runCatching {
        gson.fromJson(json, NotebookPayload::class.java)?.toEntity()
    }.getOrNull()
}
