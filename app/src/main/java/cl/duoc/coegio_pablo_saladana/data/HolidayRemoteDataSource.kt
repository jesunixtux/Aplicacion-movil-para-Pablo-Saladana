package cl.duoc.coegio_pablo_saladana.data

import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Feriado oficial de Chile usado para contextualizar el calendario escolar. */
data class ChileanHoliday(
    val date: String,
    val localName: String
) {
    /** Convierte yyyy-MM-dd al formato dd/MM/yyyy que usa el formulario de registro. */
    val formattedDate: String
        get() {
            val parts = date.split('-')
            return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else date
        }
}

/** Consume la API externa Nager.Date para feriados de Chile con respaldo local. */
object HolidayRemoteDataSource {
    private const val TIMEOUT_MS = 3_000

    suspend fun fetchHolidays(year: Int = currentYear()): List<ChileanHoliday> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("https://date.nager.at/api/v3/PublicHolidays/$year/CL")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@runCatching fallbackHolidays(year)
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseHolidays(body).ifEmpty { fallbackHolidays(year) }
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { fallbackHolidays(year) }
        }

    internal fun parseHolidays(json: String): List<ChileanHoliday> {
        return NotebookRemoteDataSource.extractJsonObjects(json.trim()).mapNotNull { obj ->
            val fields = NotebookRemoteDataSource.parseFlatJsonMap(obj)
            val date = fields["date"].orEmpty()
            val localName = fields["localName"] ?: fields["name"] ?: "Feriado"
            if (date.isBlank()) null else ChileanHoliday(date = date, localName = localName)
        }
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
