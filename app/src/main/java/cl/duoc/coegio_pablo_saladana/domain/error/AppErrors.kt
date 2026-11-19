package cl.duoc.coegio_pablo_saladana.domain.error

import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import retrofit2.HttpException

/**
 * Catálogo central de errores de la aplicación.
 *
 * [describe] traduce una excepción real a un mensaje específico para el usuario:
 * base de datos, archivos, red o API. Los repositorios guardan el motivo de cada
 * fallo en su propiedad `lastError` para que la pantalla lo muestre tal cual.
 */
object AppErrors {
    const val NOT_LOGGED_IN = "Tu sesión ya no está activa. Vuelve a iniciar sesión."
    const val DATABASE_FALLBACK = "No se pudo completar la operación en la base de datos local."

    fun describe(error: Throwable): String = when (error) {
        is HttpException -> when (error.code()) {
            400 -> "El servidor rechazó la solicitud (HTTP 400). Revisa los datos enviados."
            401, 403 -> "El servidor denegó el acceso (HTTP ${error.code()})."
            404 -> "El servidor no encontró el recurso solicitado (HTTP 404). ¿La API está actualizada?"
            in 500..599 -> "El servidor tuvo un error interno (HTTP ${error.code()}). Revisa el backend Spring Boot."
            else -> "El servidor respondió con un error HTTP ${error.code()}."
        }
        is SocketTimeoutException ->
            "El servidor tardó demasiado en responder (tiempo de espera agotado). Verifica que el backend esté encendido."
        is UnknownHostException ->
            "No se pudo resolver la dirección del servidor. Revisa tu conexión a internet o la URL del backend."
        is ConnectException ->
            "No se pudo conectar al servidor. Verifica que el backend esté encendido y en la misma red."
        is SSLException ->
            "Error de seguridad SSL/TLS al conectar con el servidor: ${error.message ?: "certificado inválido"}."
        is android.database.sqlite.SQLiteException ->
            "Error en la base de datos local: ${error.message ?: "no se pudo abrir, leer o escribir"}."
        is FileNotFoundException ->
            "No se encontró el archivo: ${error.message ?: "ruta inexistente"}."
        is IOException ->
            "Error al leer o escribir el archivo: ${error.message ?: "fallo de entrada/salida"}."
        is SecurityException ->
            "Permiso denegado: ${error.message ?: "la aplicación no tiene autorización para esta acción"}."
        else ->
            "Error inesperado (${error.javaClass.simpleName}): ${error.message ?: "sin detalle adicional"}."
    }
}
