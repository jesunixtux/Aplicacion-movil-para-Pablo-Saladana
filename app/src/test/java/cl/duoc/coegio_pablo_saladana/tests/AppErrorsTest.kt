package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AppErrorsTest {
    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun apiHttpErrorsMentionTheExactStatusCode() {
        assertTrue(AppErrors.describe(http(400)).contains("HTTP 400"))
        assertTrue(AppErrors.describe(http(401)).contains("denegó el acceso"))
        assertTrue(AppErrors.describe(http(403)).contains("HTTP 403"))
        assertTrue(AppErrors.describe(http(404)).contains("HTTP 404"))
        assertTrue(AppErrors.describe(http(500)).contains("error interno"))
        assertTrue(AppErrors.describe(http(503)).contains("HTTP 503"))
        assertTrue(AppErrors.describe(http(418)).contains("HTTP 418"))
    }

    @Test
    fun networkFailuresExplainWhatToCheck() {
        assertTrue(AppErrors.describe(SocketTimeoutException()).contains("tardó demasiado"))
        assertTrue(AppErrors.describe(UnknownHostException("date.nager.at")).contains("resolver la dirección"))
        assertTrue(AppErrors.describe(ConnectException()).contains("conectar al servidor"))
    }

    @Test
    fun fileErrorsKeepTheOriginalDetail() {
        val io = AppErrors.describe(IOException("disco lleno"))
        assertTrue(io.contains("leer o escribir"))
        assertTrue(io.contains("disco lleno"))

        val missing = AppErrors.describe(FileNotFoundException("/sdcard/foto.jpg"))
        assertTrue(missing.contains("No se encontró el archivo"))
        assertTrue(missing.contains("/sdcard/foto.jpg"))
    }

    @Test
    fun permissionErrorsAreExplicit() {
        val message = AppErrors.describe(SecurityException("cámara denegada"))
        assertTrue(message.contains("Permiso denegado"))
        assertTrue(message.contains("cámara denegada"))
    }

    @Test
    fun unexpectedErrorsShowClassAndMessageOrNullDetail() {
        val withMessage = AppErrors.describe(RuntimeException("algo raro"))
        assertTrue(withMessage.contains("Error inesperado"))
        assertTrue(withMessage.contains("RuntimeException"))
        assertTrue(withMessage.contains("algo raro"))

        val withoutMessage = AppErrors.describe(IllegalStateException())
        assertTrue(withoutMessage.contains("IllegalStateException"))
        assertTrue(withoutMessage.contains("sin detalle adicional"))
    }

    @Test
    fun sessionMessageIsSpecific() {
        assertTrue(AppErrors.NOT_LOGGED_IN.contains("sesión"))
    }
}
