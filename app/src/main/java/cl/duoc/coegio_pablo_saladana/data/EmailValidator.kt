package cl.duoc.coegio_pablo_saladana.data

internal object EmailValidator {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$")

    fun isValid(value: String): Boolean = emailPattern.matches(value.trim())
}
