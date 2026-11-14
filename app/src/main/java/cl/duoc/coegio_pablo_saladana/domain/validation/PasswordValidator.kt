package cl.duoc.coegio_pablo_saladana.domain.validation

/** Contraseñas nuevas de al menos ocho caracteres. */
object PasswordValidator {
    fun isValid(value: String): Boolean = value.length >= 8
}
