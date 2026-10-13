package cl.duoc.coegio_pablo_saladana.data

import androidx.lifecycle.ViewModel

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {
    suspend fun login(email: String, password: String): UserAccount? {
        return userRepository.login(email, password)
    }

    suspend fun restoreSession(): UserAccount? {
        return userRepository.getActiveSession()
    }

    fun saveSession(account: UserAccount) {
        userRepository.saveSession(account)
    }

    fun clearSession() {
        userRepository.clearSession()
    }

    suspend fun register(
        firstName: String,
        middleName: String,
        paternalSurname: String,
        maternalSurname: String,
        rut: String,
        email: String,
        password: String,
        role: UserRole,
        gender: UserGender
    ): UserAccount? {
        val created = userRepository.register(
            firstName,
            middleName,
            paternalSurname,
            maternalSurname,
            rut,
            email,
            password,
            role,
            gender
        )
        return if (created) userRepository.login(email, password) else null
    }
}
