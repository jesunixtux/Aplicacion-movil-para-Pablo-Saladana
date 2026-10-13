package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.PasswordHasher
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
}
