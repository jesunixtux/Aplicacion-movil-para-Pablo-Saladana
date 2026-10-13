package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.RutValidator
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
    }

    @Test
    fun acceptsValidRut() {
        assertTrue(RutValidator.isValid("12.345.678-5"))
        assertTrue(RutValidator.isValid("11.111.111-1"))
        assertTrue(RutValidator.isValid("1.000.005-K"))
    }

    @Test
    fun rejectsInvalidRut() {
        assertFalse(RutValidator.isValid("12.345.678-4"))
        assertFalse(RutValidator.isValid("123.456.789-0"))
    }
}
