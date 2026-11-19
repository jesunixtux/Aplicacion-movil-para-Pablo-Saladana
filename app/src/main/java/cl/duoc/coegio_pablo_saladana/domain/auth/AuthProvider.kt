package cl.duoc.coegio_pablo_saladana.domain.auth

import cl.duoc.coegio_pablo_saladana.data.repository.UserRepository
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount

/**
 * Identidad del usuario separada del almacenamiento.
 *
 * [UserAccount] ya es la identidad (nunca contiene contraseña ni hash); el proveedor
 * decide contra qué almacenamiento se valida. Hoy solo existe el local (Room).
 * Los roles Student/Teacher y el curso/asignatura viajan dentro de la identidad.
 */
sealed interface AuthOutcome {
    data class Success(val account: UserAccount) : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

/** Contrato común para autenticación local (Room) y futura remota (Spring Boot). */
interface AuthProvider {
    /** true si este proveedor valida contra un servidor remoto. */
    val isRemote: Boolean

    suspend fun login(email: String, password: String): AuthOutcome
}

/** Proveedor activo: valida contra Room; la contraseña nunca sale del dispositivo. */
class LocalAuthProvider(private val userRepository: UserRepository) : AuthProvider {
    override val isRemote: Boolean = false

    override suspend fun login(email: String, password: String): AuthOutcome {
        val account = userRepository.login(email, password)
        return if (account != null) {
            AuthOutcome.Success(account)
        } else {
            AuthOutcome.Failure(userRepository.lastError ?: "El correo o la contraseña no son correctos.")
        }
    }
}

/**
 * Proveedor remoto PREPARADO pero DESACTIVADO.
 *
 * Cuando el backend implemente `POST /api/auth/login` (contrato propuesto en
 * PREPARACION-MIGRACION-BACKEND.md), este proveedor recibirá la respuesta con la
 * identidad y el token de sesión, y el token se entregará al
 * `SessionTokenInterceptor`. No existe ningún token ficticio: mientras tanto,
 * cualquier intento falla con un mensaje explícito y la app sigue usando Room.
 */
class RemoteAuthProvider : AuthProvider {
    override val isRemote: Boolean = true

    override suspend fun login(email: String, password: String): AuthOutcome =
        AuthOutcome.Failure(
            "La autenticación remota aún no está habilitada: el backend no implementa el endpoint de acceso. " +
                "Usa el inicio de sesión local."
        )
}