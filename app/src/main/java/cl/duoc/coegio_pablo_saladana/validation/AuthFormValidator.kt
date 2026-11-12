package cl.duoc.coegio_pablo_saladana.validation

/** Errores por campo que las pantallas de acceso muestran al usuario. */
object AuthFormValidator {
    data class LoginErrors(val email: Boolean, val password: Boolean) {
        val isValid: Boolean get() = !email && !password
    }

    data class RegisterErrors(
        val firstName: Boolean,
        val middleName: Boolean,
        val paternalSurname: Boolean,
        val maternalSurname: Boolean,
        val rut: Boolean,
        val email: Boolean,
        val password: Boolean,
        val confirmPassword: Boolean
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
