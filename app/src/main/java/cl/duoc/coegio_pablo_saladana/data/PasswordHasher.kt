package cl.duoc.coegio_pablo_saladana.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Genera hashes con sal y verifica tanto el formato actual como el legado para migrarlo al iniciar sesión. */
internal object PasswordHasher {
    private const val PREFIX = "pbkdf2"
    private const val SALT_SIZE = 16
    private const val HASH_SIZE_BITS = 256
    private const val SHA256_ITERATIONS = 600_000
    private const val SHA1_ITERATIONS = 1_400_000
    private const val MAX_STORED_ITERATIONS = 2_000_000
    private const val HEX_DIGITS = "0123456789abcdef"

    private val random = SecureRandom()
    private val legacyHashPattern = Regex("[0-9a-fA-F]{64}")

    fun create(password: String): String {
        val algorithm = if (runCatching {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            }.isSuccess
        ) {
            Algorithm.SHA256
        } else {
            Algorithm.SHA1
        }
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val hash = derive(password, salt, algorithm.iterations, algorithm.factoryName)
        return listOf(PREFIX, algorithm.id, algorithm.iterations, salt.toHex(), hash.toHex())
            .joinToString(":")
    }

    fun verify(password: String, storedHash: String): Boolean {
        if (isLegacyHash(storedHash)) {
            val expected = storedHash.hexToBytes() ?: return false
            val actual = MessageDigest.getInstance("SHA-256")
                .digest(password.toByteArray(StandardCharsets.UTF_8))
            return MessageDigest.isEqual(expected, actual)
        }

        val parts = storedHash.split(':')
        if (parts.size != 5 || parts[0] != PREFIX) return false
        val algorithm = Algorithm.entries.firstOrNull { it.id == parts[1] } ?: return false
        val iterations = parts[2].toIntOrNull() ?: return false
        if (iterations !in 1..MAX_STORED_ITERATIONS) return false
        val salt = parts[3].hexToBytes()?.takeIf { it.size == SALT_SIZE } ?: return false
        val expected = parts[4].hexToBytes()?.takeIf { it.size == HASH_SIZE_BITS / 8 } ?: return false
        val actual = runCatching {
            derive(password, salt, iterations, algorithm.factoryName)
        }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    fun isLegacyHash(storedHash: String): Boolean = legacyHashPattern.matches(storedHash)

    private fun derive(
        password: String,
        salt: ByteArray,
        iterations: Int,
        factoryName: String
    ): ByteArray {
        val passwordChars = password.toCharArray()
        val spec = PBEKeySpec(passwordChars, salt, iterations, HASH_SIZE_BITS)
        return try {
            SecretKeyFactory.getInstance(factoryName).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            passwordChars.fill('\u0000')
        }
    }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and 0xff
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0f])
        }
    }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0 || any { it.digitToIntOrNull(16) == null }) return null
        return ByteArray(length / 2) { index ->
            val high = this[index * 2].digitToInt(16)
            val low = this[index * 2 + 1].digitToInt(16)
            ((high shl 4) or low).toByte()
        }
    }

    private enum class Algorithm(val id: String, val factoryName: String, val iterations: Int) {
        SHA256("sha256", "PBKDF2WithHmacSHA256", SHA256_ITERATIONS),
        SHA1("sha1", "PBKDF2WithHmacSHA1", SHA1_ITERATIONS)
    }
}
