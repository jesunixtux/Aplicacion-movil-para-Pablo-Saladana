package cl.duoc.coegio_pablo_saladana.domain.auth

import cl.duoc.coegio_pablo_saladana.data.repository.UserRepository
import cl.duoc.coegio_pablo_saladana.data.remote.LoginInput
import cl.duoc.coegio_pablo_saladana.data.remote.RegisterInput
import cl.duoc.coegio_pablo_saladana.data.remote.SchoolBackend
import cl.duoc.coegio_pablo_saladana.data.remote.auth.RemoteSessionStore
import cl.duoc.coegio_pablo_saladana.data.remote.backendMessage
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole

/**
 * Identidad del usuario separada del almacenamiento.
 *
 * [UserAccount] ya es la identidad (nunca contiene contraseña ni hash); el proveedor
 * decide contra qué almacenamiento se valida.
 * Los roles Student/Teacher y el curso/asignatura viajan dentro de la identidad.
 */
sealed interface AuthOutcome {
    data class Success(val account: UserAccount) : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

/** Contrato común para autenticación local (Room) y remota (Spring Boot). */
interface AuthProvider {
    /** true si este proveedor valida contra un servidor remoto. */
    val isRemote: Boolean

    suspend fun login(email: String, password: String): AuthOutcome
    suspend fun register(data: RegistrationData): AuthOutcome
    suspend fun restoreSession(): UserAccount?
    suspend fun logout()
}

data class RegistrationData(
    val firstName: String,
    val middleName: String,
    val paternalSurname: String,
    val maternalSurname: String,
    val rut: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val gender: UserGender,
    val course: String
) {
    val fullName: String get() = listOf(firstName, middleName, paternalSurname, maternalSurname)
        .filter(String::isNotBlank).joinToString(" ")
}

/** Proveedor de compatibilidad local; solo se usa si la sincronización remota se desactiva. */
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

    override suspend fun register(data: RegistrationData): AuthOutcome {
        val account = userRepository.register(
            data.firstName, data.middleName, data.paternalSurname, data.maternalSurname,
            data.rut, data.email, data.password, data.role, data.gender, data.course
        )
        return account?.let(AuthOutcome::Success)
            ?: AuthOutcome.Failure(userRepository.lastError ?: "No se pudo crear la cuenta.")
    }

    override suspend fun restoreSession(): UserAccount? = userRepository.getActiveSession()
    override suspend fun logout() = userRepository.clearSession()
}

/** Autenticación real contra Spring Boot, con identidad verificada por `/api/auth/me`. */
class RemoteAuthProvider(private val sessions: RemoteSessionStore) : AuthProvider {
    override val isRemote: Boolean = true

    init { SchoolBackend.configure(sessions) }

    override suspend fun login(email: String, password: String): AuthOutcome = authenticate {
        SchoolBackend.api.login(LoginInput(email.trim().lowercase(), password))
    }

    override suspend fun register(data: RegistrationData): AuthOutcome {
        if (data.role != UserRole.Student) {
            return AuthOutcome.Failure("Las cuentas docentes solo se crean en el backend por un administrador.")
        }
        return authenticate {
            SchoolBackend.api.register(RegisterInput(data.fullName, data.email.trim().lowercase(), data.password, data.course))
        }
    }

    override suspend fun restoreSession(): UserAccount? {
        val cached = sessions.currentUser() ?: return null
        return runCatching {
            val verified = SchoolBackend.api.me()
            sessions.updateUser(verified)
            verified.toAccount()
        }.getOrElse {
            if (!SchoolBackend.hasSession()) null else cached
        }
    }

    override suspend fun logout() {
        if (sessions.currentToken() != null) runCatching { SchoolBackend.api.logout() }
        sessions.clear()
    }

    private suspend fun authenticate(
        request: suspend () -> cl.duoc.coegio_pablo_saladana.data.remote.auth.AuthResponseDto
    ): AuthOutcome {
        if (!SchoolBackend.remoteEnabled) return AuthOutcome.Failure(cl.duoc.coegio_pablo_saladana.domain.error.AppErrors.REMOTE_DISABLED)
        return runCatching {
            val response = request()
            sessions.save(response)
            val verified = SchoolBackend.api.me()
            sessions.updateUser(verified)
            verified.toAccount()?.let(AuthOutcome::Success)
                ?: AuthOutcome.Failure("El servidor devolvió una identidad inválida o inactiva.")
        }.getOrElse {
            sessions.clear()
            AuthOutcome.Failure(backendMessage("autenticación", it))
        }
    }
}
