package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.PasswordHasher
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun newHashesHaveDifferentSaltsAndVerifyOnlyTheRightPassword() {
        val first = PasswordHasher.create("Clave de prueba 123")
        val second = PasswordHasher.create("Clave de prueba 123")

        assertNotEquals(first, second)
        assertTrue(first.startsWith("pbkdf2:"))
        assertTrue(PasswordHasher.verify("Clave de prueba 123", first))
        assertFalse(PasswordHasher.verify("Clave incorrecta", first))
        assertFalse(PasswordHasher.isLegacyHash(first))
    }

    @Test
    fun oldSha256HashRemainsValidForMigration() {
        val oldHash = MessageDigest.getInstance("SHA-256")
            .digest("Contraseña anterior".toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        assertTrue(PasswordHasher.isLegacyHash(oldHash))
        assertTrue(PasswordHasher.verify("Contraseña anterior", oldHash))
        assertFalse(PasswordHasher.verify("Otra contraseña", oldHash))
    }

    @Test
    fun malformedStoredHashesAreRejected() {
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:-1:00:00"))
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:999999999:00:00"))
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:600000:xx:yy"))
        assertFalse(PasswordHasher.verify("clave", "hash-desconocido"))
    }

    @Test
    fun emptyAndUnicodePasswordsRoundTrip() {
        val empty = PasswordHasher.create("")
        assertTrue(PasswordHasher.verify("", empty))
        assertFalse(PasswordHasher.verify("x", empty))

        val unicode = PasswordHasher.create("Clave ñandú 🔑")
        assertTrue(PasswordHasher.verify("Clave ñandú 🔑", unicode))
        assertFalse(PasswordHasher.verify("Clave nandu", unicode))
    }

    @Test
    fun storedHashStructureIsValidatedBeforeDeriving() {
        val salt = "00".repeat(16)
        val hash = "00".repeat(32)
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:0:$salt:$hash"))
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:2000001:$salt:$hash"))
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:md5:600000:$salt:$hash"))
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:600000:${"0".repeat(31)}:$hash"))
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:600000:$salt:${"0".repeat(62)}"))
        assertFalse(PasswordHasher.verify("clave", "pbkdf2:sha256:600000:$salt"))
    }

    @Test
    fun legacyDetectionRequiresExactlySixtyFourHexCharacters() {
        assertTrue(PasswordHasher.isLegacyHash("a".repeat(64)))
        assertTrue(PasswordHasher.isLegacyHash("ABCDEF0123456789".repeat(4)))
        assertFalse(PasswordHasher.isLegacyHash("a".repeat(63)))
        assertFalse(PasswordHasher.isLegacyHash("a".repeat(65)))
        assertFalse(PasswordHasher.isLegacyHash("g".repeat(64)))
        assertFalse(PasswordHasher.isLegacyHash(""))
    }
}
