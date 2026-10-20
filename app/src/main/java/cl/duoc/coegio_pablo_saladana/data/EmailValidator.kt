package cl.duoc.coegio_pablo_saladana.data

/** Valida el formato básico de correos usados al registrar e iniciar sesión. */
internal object EmailValidator {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$")

    fun isValid(value: String): Boolean = emailPattern.matches(value.trim())
}
