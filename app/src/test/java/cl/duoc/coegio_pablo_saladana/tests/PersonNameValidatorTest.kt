package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.validation.PersonNameValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonNameValidatorTest {
    @Test
    fun acceptsLettersSpacesAndHyphens() {
        assertTrue(PersonNameValidator.isValid("Ana"))
        assertTrue(PersonNameValidator.isValid("José Miguel"))
        assertTrue(PersonNameValidator.isValid("Pérez-Soto"))
        assertTrue(PersonNameValidator.isValid("  Ana  "))
    }

    @Test
    fun rejectsEmptyDigitsAndSymbols() {
        assertFalse(PersonNameValidator.isValid(""))
        assertFalse(PersonNameValidator.isValid("   "))
        assertFalse(PersonNameValidator.isValid("Ana2"))
        assertFalse(PersonNameValidator.isValid("Ana@"))
        assertFalse(PersonNameValidator.isValid("Ana  Pérez"))
        assertFalse(PersonNameValidator.isValid("-Ana"))
    }

    @Test
    fun sanitizeKeepsOnlyLettersSpacesAndHyphens() {
        assertEquals("Ana Pérez", PersonNameValidator.sanitize("Ana Pérez123"))
        assertEquals("Pérez-Soto", PersonNameValidator.sanitize("Pérez-Soto!@#"))
        assertEquals("", PersonNameValidator.sanitize("12345"))
    }
}
