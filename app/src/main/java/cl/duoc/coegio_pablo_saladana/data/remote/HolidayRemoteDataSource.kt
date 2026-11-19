package cl.duoc.coegio_pablo_saladana.data.remote

import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

private data class HolidayPayload(val date: String?, val localName: String?, val name: String?) {
    fun toHoliday(): ChileanHoliday? = date?.takeIf(String::isNotBlank)?.let {
        ChileanHoliday(it, localName ?: name ?: "Feriado")
    }
}

private interface HolidayApi {
    @GET("api/v3/PublicHolidays/{year}/CL")
    suspend fun forYear(@Path("year") year: Int): List<HolidayPayload>
}

/** Consume la API externa Nager.Date para feriados de Chile con respaldo local. */
object HolidayRemoteDataSource {
    /** Motivo del último fallo de la API de feriados; null si respondió bien. */
    var lastError: String? = null
        private set

    private val api = Retrofit.Builder()
        .baseUrl("https://date.nager.at/")
        .client(okhttp3.OkHttpClient.Builder()
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(HolidayApi::class.java)

    suspend fun fetchHolidays(year: Int = currentYear()): List<ChileanHoliday> {
        lastError = null
        val fetched = runCatching { api.forYear(year).mapNotNull(HolidayPayload::toHoliday) }
            .onFailure {
                lastError = "No se pudo consultar la API de feriados (date.nager.at). " +
                    cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.describe(it)
            }
            .getOrNull()
        return fetched?.ifEmpty { fallbackHolidays(year) } ?: fallbackHolidays(year)
    }

    internal fun parseHolidays(json: String): List<ChileanHoliday> {
        val type = object : TypeToken<List<HolidayPayload>>() {}.type
        return runCatching {
            (Gson().fromJson<List<HolidayPayload>>(json, type) ?: emptyList())
                .mapNotNull(HolidayPayload::toHoliday)
        }.getOrDefault(emptyList())
    }

    internal fun findHolidayForClassDate(
        classDate: String,
        holidays: List<ChileanHoliday>
    ): ChileanHoliday? {
        val normalized = classDate.trim()
        return holidays.firstOrNull { it.formattedDate == normalized || it.date == normalized }
    }

    // Respaldo offline para garantizar demostración funcional sin internet durante la defensa.
    internal fun fallbackHolidays(year: Int): List<ChileanHoliday> = listOf(
        ChileanHoliday("$year-01-01", "Año Nuevo"),
        ChileanHoliday("$year-05-01", "Día Nacional del Trabajo"),
        ChileanHoliday("$year-05-21", "Día de las Glorias Navales"),
        ChileanHoliday("$year-06-20", "Día Nacional de los Pueblos Indígenas"),
        ChileanHoliday("$year-07-16", "Día de la Virgen del Carmen"),
        ChileanHoliday("$year-08-15", "Asunción de la Virgen"),
        ChileanHoliday("$year-09-18", "Independencia Nacional"),
        ChileanHoliday("$year-09-19", "Día de las Glorias del Ejército"),
        ChileanHoliday("$year-10-12", "Encuentro de Dos Mundos"),
        ChileanHoliday("$year-10-31", "Día de las Iglesias Evangélicas y Protestantes"),
        ChileanHoliday("$year-11-01", "Día de Todos los Santos"),
        ChileanHoliday("$year-12-08", "Inmaculada Concepción"),
        ChileanHoliday("$year-12-25", "Navidad")
    )

    private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
