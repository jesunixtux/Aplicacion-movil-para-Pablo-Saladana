package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.EmailValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {
    @Test
    fun acceptsRegularAndPlusAddressedEmails() {
        assertTrue(EmailValidator.isValid("estudiante@escuela.cl"))
        assertTrue(EmailValidator.isValid("docente+curso@sub.escuela.cl"))
    }

    @Test
    fun rejectsIncompleteAddresses() {
        assertFalse(EmailValidator.isValid("estudiante@"))
        assertFalse(EmailValidator.isValid("estudiante@escuela"))
        assertFalse(EmailValidator.isValid("estudiante@@escuela.cl"))
        assertFalse(EmailValidator.isValid("estudiante @escuela.cl"))
    }
}
