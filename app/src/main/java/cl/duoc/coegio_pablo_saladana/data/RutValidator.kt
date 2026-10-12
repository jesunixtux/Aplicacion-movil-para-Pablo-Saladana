package cl.duoc.coegio_pablo_saladana.data

object RutValidator {
    fun format(value: String): String {
        val cleanValue = value.uppercase().filter { it.isDigit() || it == 'K' }
        if (cleanValue.length <= 1) return cleanValue

        val body = cleanValue.dropLast(1).take(8)
        val verifier = cleanValue.last()
        val groupedBody = body.reversed().chunked(3).joinToString(".").reversed()
        return "$groupedBody-$verifier"
    }

    fun isValid(value: String): Boolean {
        val cleanValue = value.uppercase().filter { it.isDigit() || it == 'K' }
        if (cleanValue.length !in 8..9) return false

        val body = cleanValue.dropLast(1)
        val verifier = cleanValue.last()
        if (body.any { !it.isDigit() } || body.all { it == '0' }) return false

        var multiplier = 2
        var sum = 0
        for (digit in body.reversed()) {
            sum += digit.digitToInt() * multiplier
            multiplier = if (multiplier == 7) 2 else multiplier + 1
        }

        val remainder = 11 - (sum % 11)
        val expectedVerifier = when (remainder) {
            11 -> '0'
            10 -> 'K'
            else -> remainder.digitToChar()
        }
        return verifier == expectedVerifier
    }
}
