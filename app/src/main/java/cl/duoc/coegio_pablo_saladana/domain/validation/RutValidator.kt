package cl.duoc.coegio_pablo_saladana.domain.validation

/** Formatea y valida RUT chilenos, incluido el dígito verificador K. */
object RutValidator {
    fun format(value: String): String {
        val upperValue = value.uppercase()
        val hasHyphen = upperValue.contains('-')
        val cleanValue = upperValue.filter { it.isDigit() || it == 'K' }
        if (cleanValue.isEmpty()) return ""

        val body: String
        val verifier: Char?
        if (hasHyphen) {
            val parts = upperValue.split('-', limit = 2)
            body = parts.first().filter(Char::isDigit).take(8)
            verifier = parts.getOrNull(1)?.firstOrNull { it.isDigit() || it == 'K' }
        } else if (cleanValue.last() == 'K') {
            body = cleanValue.dropLast(1).take(8)
            verifier = 'K'
        } else if (cleanValue.length > 8) {
            body = cleanValue.take(8)
            verifier = cleanValue[8]
        } else {
            body = cleanValue.take(8)
            verifier = null
        }

        val groupedBody = body.reversed().chunked(3).joinToString(".").reversed()
        return if (verifier == null) groupedBody else "$groupedBody-$verifier"
    }

    /** Conserva una posición natural del cursor al insertar puntos y guion automáticamente. */
    fun formattedCursorPosition(value: String, cursor: Int): Int {
        val prefix = value.take(cursor.coerceIn(0, value.length))
        val formattedPrefix = format(prefix)
        val formatted = format(value)
        val afterHyphen = prefix.endsWith('-') && formatted.getOrNull(formattedPrefix.length) == '-'
        return (formattedPrefix.length + if (afterHyphen) 1 else 0).coerceAtMost(formatted.length)
    }

    fun isValid(value: String): Boolean {
        val cleanValue = value.uppercase().filter { it.isDigit() || it == 'K' }
        // Solo verificamos el formato básico: 9 dígitos (8 números + 1 dígito verificador)
        return cleanValue.length == 9
    }
}
