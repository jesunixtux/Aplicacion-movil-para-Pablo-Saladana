package cl.duoc.coegio_pablo_saladana.domain.validation

/** Errores por campo que las pantallas de acceso muestran al usuario. */
object AuthFormValidator {
    data class LoginErrors(
        val email: Boolean = false,
        val password: Boolean = false
    ) {
        val isValid: Boolean get() = !email && !password
    }

    data class RegisterErrors(
        val firstName: Boolean = false,
        val middleName: Boolean = false,
        val paternalSurname: Boolean = false,
        val maternalSurname: Boolean = false,
        val rut: Boolean = false,
        val email: Boolean = false,
        val password: Boolean = false,
        val confirmPassword: Boolean = false
    ) {
        val isValid: Boolean get() = !firstName && !middleName && !paternalSurname &&
            !maternalSurname && !rut && !email && !password && !confirmPassword
    }

    fun login(email: String, password: String): LoginErrors = LoginErrors(
        email = !EmailValidator.isValid(email),
        password = password.isBlank()
    )

    fun register(
        firstName: String,
        middleName: String,
        paternalSurname: String,
        maternalSurname: String,
        rut: String,
        email: String,
        password: String,
        confirmPassword: String
    ): RegisterErrors = RegisterErrors(
        firstName = !PersonNameValidator.isValid(firstName),
        middleName = middleName.isNotBlank() && !PersonNameValidator.isValid(middleName),
        paternalSurname = !PersonNameValidator.isValid(paternalSurname),
        maternalSurname = !PersonNameValidator.isValid(maternalSurname),
        rut = !RutValidator.isValid(rut),
        email = !EmailValidator.isValid(email),
        password = !PasswordValidator.isValid(password),
        confirmPassword = confirmPassword.isBlank() || confirmPassword != password
    )
}
