package cl.duoc.coegio_pablo_saladana.data.remote.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Fuente del token de sesión remoto. La implementación por defecto devuelve null
 * (sin sesión remota): no hay credenciales ficticias incrustadas en ningún lugar.
 * Cuando exista login remoto, guardará el token entregado por el backend.
 */
fun interface SessionTokenProvider {
    fun currentToken(): String?
}

/**
 * Adjunta `Authorization: Bearer <token>` solo si hay un token real de sesión.
 * Sin token, la petición sale sin cabecera de autenticación: el filtrado por
 * correo del backend actual NO se considera autenticación (ver documentación).
 */
class SessionTokenInterceptor(
    private val tokenProvider: SessionTokenProvider,
    private val onUnauthorized: () -> Unit = {}
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.currentToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request).also { response ->
            if (response.code == 401 && !token.isNullOrBlank()) onUnauthorized()
        }
    }
}
