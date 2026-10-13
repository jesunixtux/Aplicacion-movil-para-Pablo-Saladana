package cl.duoc.coegio_pablo_saladana.data

import androidx.lifecycle.ViewModel

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {
    fun login(email: String, password: String): UserAccount? {
        return userRepository.login(email, password)
    }

    fun register(
        firstName: String,
        middleName: String,
        paternalSurname: String,
        maternalSurname: String,
        rut: String,
        email: String,
        password: String,
        role: UserRole
    ): UserAccount? {
        val created = userRepository.register(
            firstName,
            middleName,
            paternalSurname,
            maternalSurname,
            rut,
            email,
            password,
            role
        )
        return if (created) userRepository.login(email, password) else null
    }
}
