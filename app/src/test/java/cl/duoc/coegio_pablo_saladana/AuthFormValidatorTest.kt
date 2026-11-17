package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.domain.validation.AuthFormValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFormValidatorTest {
    @Test
    fun loginReportsInvalidFieldsSeparately() {
        val errors = AuthFormValidator.login("correo-invalido", "")
        assertTrue(errors.email)
        assertTrue(errors.password)
        assertFalse(errors.isValid)
        assertTrue(AuthFormValidator.login("persona@escuela.cl", "clave").isValid)
    }

    @Test
    fun registrationKeepsSecondNameOptionalAndChecksConfirmation() {
        val valid = AuthFormValidator.register(
            "Ana", "", "Pérez", "Soto", "12345678K", "ana@escuela.cl", "clave123", "clave123"
        )
        assertTrue(valid.isValid)

        val errors = AuthFormValidator.register(
            "", "Ana2", "Pérez", "Soto", "12345678X", "correo", "corta", "distinta"
        )
        assertTrue(errors.firstName)
        assertTrue(errors.middleName)
        assertTrue(errors.rut)
        assertTrue(errors.email)
        assertTrue(errors.password)
        assertTrue(errors.confirmPassword)
        assertFalse(errors.isValid)
    }
}
