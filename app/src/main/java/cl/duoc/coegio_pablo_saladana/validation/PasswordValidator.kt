package cl.duoc.coegio_pablo_saladana.validation

/** Contraseñas nuevas de al menos ocho caracteres. */
object PasswordValidator {
    fun isValid(value: String): Boolean = value.length >= 8
}
