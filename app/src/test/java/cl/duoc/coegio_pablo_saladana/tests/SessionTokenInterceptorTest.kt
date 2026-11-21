package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.remote.auth.SessionTokenInterceptor
import cl.duoc.coegio_pablo_saladana.data.remote.auth.SessionTokenProvider
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTokenInterceptorTest {
    /** Chain de prueba que registra la petición que realmente salió. */
    private class RecordingChain(private val initial: Request, private val responseCode: Int = 200) : Interceptor.Chain {
        var proceeded: Request? = null

        override fun request(): Request = initial
        override fun proceed(request: Request): Response {
            proceeded = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(responseCode)
                .message("OK")
                .build()
        }
        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    private val baseRequest = Request.Builder().url("http://10.0.2.2:8090/api/notebooks").build()

    @Test
    fun withoutTokenNoAuthorizationHeaderIsAttached() {
        val chain = RecordingChain(baseRequest)
        SessionTokenInterceptor(SessionTokenProvider { null }).intercept(chain)
        assertNull(chain.proceeded!!.header("Authorization"))
    }

    @Test
    fun blankTokenIsTreatedAsNoSession() {
        val chain = RecordingChain(baseRequest)
        SessionTokenInterceptor(SessionTokenProvider { "   " }).intercept(chain)
        assertNull(chain.proceeded!!.header("Authorization"))
    }

    @Test
    fun realSessionTokenIsAttachedAsBearer() {
        val chain = RecordingChain(baseRequest)
        SessionTokenInterceptor(SessionTokenProvider { "token-real-del-backend" }).intercept(chain)
        assertEquals("Bearer token-real-del-backend", chain.proceeded!!.header("Authorization"))
    }

    @Test
    fun defaultProviderHasNoEmbeddedCredentials() {
        // El proveedor por defecto de la app no entrega ningún token ficticio.
        assertNull(SessionTokenProvider { null }.currentToken())
    }

    @Test
    fun unauthorizedResponseInvalidatesTheRemoteSession() {
        var invalidated = false
        val chain = RecordingChain(baseRequest, responseCode = 401)
        SessionTokenInterceptor(SessionTokenProvider { "expired-token" }) { invalidated = true }.intercept(chain)
        assertEquals(true, invalidated)
    }
}
