package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.domain.validation.PasswordValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {
    @Test
    fun requiresAtLeastEightCharacters() {
        assertFalse(PasswordValidator.isValid("1234567"))
        assertTrue(PasswordValidator.isValid("12345678"))
    }
}
