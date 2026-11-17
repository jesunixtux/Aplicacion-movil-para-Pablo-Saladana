package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.domain.validation.RutValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RutValidatorTest {
    @Test
    fun formatsRutWithDotsAndHyphen() {
        assertEquals("12.345.678-5", RutValidator.format("123456785"))
        assertEquals("12.345.678", RutValidator.format("12345678"))
        assertEquals("1.000.005-K", RutValidator.format("1000005K"))
        assertEquals("23.567.224-K", RutValidator.format("23.567.224-k"))
    }

    @Test
    fun keepsCursorPositionWhenFormatting() {
        assertEquals(5, RutValidator.formattedCursorPosition("23567224", 4))
        assertEquals(11, RutValidator.formattedCursorPosition("23567224-K", 9))
    }

    @Test
    fun acceptsValidRut() {
        assertTrue(RutValidator.isValid("123456785"))
        assertTrue(RutValidator.isValid("111111111"))
        assertTrue(RutValidator.isValid("100000555"))
        assertTrue(RutValidator.isValid("235672243"))
    }

    @Test
    fun rejectsInvalidRut() {
        assertFalse(RutValidator.isValid("12345678"))  // Too short
        assertFalse(RutValidator.isValid("1234567890"))  // Too long
        assertFalse(RutValidator.isValid("12345678901"))  // Too long
        assertFalse(RutValidator.isValid("2356722455"))  // Too long
    }
}
