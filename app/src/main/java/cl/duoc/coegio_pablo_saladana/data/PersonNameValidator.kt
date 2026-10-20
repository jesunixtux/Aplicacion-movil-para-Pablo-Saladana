package cl.duoc.coegio_pablo_saladana.data

/** Acepta nombres con letras, espacios y guiones; elimina otros caracteres durante la escritura. */
object PersonNameValidator {
    private val namePattern = Regex("^[\\p{L}]+(?:[ -][\\p{L}]+)*$")

    fun isValid(value: String): Boolean {
        return value.trim().matches(namePattern)
    }

    fun sanitize(value: String): String {
        return value.filter { it.isLetter() || it == ' ' || it == '-' }
    }
}
