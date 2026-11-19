package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.validation.AuthFormValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.EmailValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.PasswordValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.RutValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.StudentEntryValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos límite de cada validador, verificados contra su implementación real. */
class ValidatorEdgeCasesTest {
    // --- EmailValidator ---
    @Test
    fun emailTrimsSurroundingSpaces() {
        assertTrue(EmailValidator.isValid(" ana@escuela.cl "))
    }

    @Test
    fun emailAcceptsMinimalValidShape() {
        assertTrue(EmailValidator.isValid("a@b.c"))
        assertTrue(EmailValidator.isValid("ANA@ESCUELA.CL"))
    }

    @Test
    fun emailRejectsBrokenShapes() {
        assertFalse(EmailValidator.isValid("@b.cl"))
        assertFalse(EmailValidator.isValid("a@.cl"))
        assertFalse(EmailValidator.isValid("a@b..cl"))
        assertFalse(EmailValidator.isValid("a@b.cl."))
        assertFalse(EmailValidator.isValid("a b@c.cl"))
        assertFalse(EmailValidator.isValid(""))
    }

    // --- PasswordValidator ---
    @Test
    fun passwordOnlyChecksLength() {
        assertFalse(PasswordValidator.isValid(""))
        assertFalse(PasswordValidator.isValid("1234567"))
        assertTrue(PasswordValidator.isValid("12345678"))
        // Comportamiento real: ocho espacios cuentan como ocho caracteres.
        assertTrue(PasswordValidator.isValid("        "))
    }

    // --- RutValidator.format ---
    @Test
    fun formatReturnsEmptyForNonRutInput() {
        assertEquals("", RutValidator.format(""))
        assertEquals("", RutValidator.format("abc"))
        assertEquals("", RutValidator.format("---"))
    }

    @Test
    fun formatHandlesHyphenLowercaseKAndExtraDigits() {
        assertEquals("12.345.678-5", RutValidator.format("12345678-5"))
        assertEquals("12.345.678-K", RutValidator.format("12345678-k"))
        assertEquals("1.000.005-K", RutValidator.format("1000005k"))
        // Más de 8 dígitos: el noveno carácter pasa a ser el verificador.
        assertEquals("12.345.678-9", RutValidator.format("1234567890"))
    }

    @Test
    fun formatIsIdempotentOnAlreadyFormattedRut() {
        assertEquals("12.345.678-5", RutValidator.format("12.345.678-5"))
        assertEquals("23.567.224-K", RutValidator.format("23.567.224-K"))
    }

    @Test
    fun cursorPositionClampsOutOfRangeValues() {
        assertEquals(0, RutValidator.formattedCursorPosition("23567224", -3))
        assertEquals(10, RutValidator.formattedCursorPosition("23567224", 999))
        assertEquals(0, RutValidator.formattedCursorPosition("", 0))
    }

    @Test
    fun rutValidityOnlyCountsNineCleanCharacters() {
        assertTrue(RutValidator.isValid("12.345.678-5"))
        assertTrue(RutValidator.isValid("12345678K"))
        assertTrue(RutValidator.isValid("12345678k"))
        assertFalse(RutValidator.isValid("12345678"))
        assertFalse(RutValidator.isValid(""))
    }

    // --- NotebookValidator ---
    @Test
    fun notebookAcceptsExplicitCourseSubjectAndPeriodBoundaries() {
        val draft = NotebookDraft(
            "T", "P", "I", "Q",
            course = "6° Básico A", subject = "Matemática", month = 1, year = 2020
        )
        assertTrue(NotebookValidator.isValid(draft))
        assertTrue(NotebookValidator.isValid(draft.copy(month = 12, year = 2100)))
    }

    @Test
    fun notebookFlagsUnknownCourseAndSubject() {
        val base = NotebookDraft("T", "P", "I", "Q")
        assertTrue(NotebookValidator.validate(base.copy(course = "General")).course)
        assertTrue(NotebookValidator.validate(base.copy(course = "Curso inventado")).course)
        assertTrue(NotebookValidator.validate(base.copy(subject = "Recreo")).subject)
    }

    @Test
    fun notebookFlagsOutOfRangePeriod() {
        val base = NotebookDraft("T", "P", "I", "Q", month = 5, year = 2026)
        assertTrue(NotebookValidator.validate(base.copy(month = 0)).period)
        assertTrue(NotebookValidator.validate(base.copy(month = 13)).period)
        assertTrue(NotebookValidator.validate(base.copy(year = 2019)).period)
        assertTrue(NotebookValidator.validate(base.copy(year = 2101)).period)
        assertFalse(NotebookValidator.validate(base).period)
    }

    // --- StudentEntryValidator ---
    @Test
    fun classDateRejectsImpossibleAndMalformedDates() {
        fun dated(date: String) = StudentEntryDraft(date, "Tema", "Actividad", "Aprendizaje", "")
        assertFalse(StudentEntryValidator.hasValidClassDate(dated("29/02/2025")))
        assertTrue(StudentEntryValidator.hasValidClassDate(dated("29/02/2024")))
        assertFalse(StudentEntryValidator.hasValidClassDate(dated("00/01/2026")))
        assertFalse(StudentEntryValidator.hasValidClassDate(dated("01/13/2026")))
        assertFalse(StudentEntryValidator.hasValidClassDate(dated("18/10/2026x")))
        assertFalse(StudentEntryValidator.hasValidClassDate(dated(" 18/10/2026")))
    }

    @Test
    fun linksCheckEveryNonBlankLine() {
        val base = StudentEntryDraft("18/10/2026", "Tema", "Actividad", "Aprendizaje", "")
        assertFalse(StudentEntryValidator.validateSubmission(base.copy(links = "https://a.cl\n\nhttps://b.cl")).links)
        assertTrue(StudentEntryValidator.validateSubmission(base.copy(links = "https://a.cl\nftp://b.cl")).links)
        assertTrue(StudentEntryValidator.validateSubmission(base.copy(links = "mailto:a@b.cl")).links)
        assertFalse(StudentEntryValidator.validateSubmission(base.copy(links = "  ")).links)
    }

    @Test
    fun doubtsAloneCountAsContent() {
        assertTrue(StudentEntryValidator.hasContent(StudentEntryDraft("", "", "", "", "¿Por qué?")))
    }

    @Test
    fun fullyBlankDraftFlagsEveryRequiredField() {
        val errors = StudentEntryValidator.validateSubmission(StudentEntryDraft("", "", "", "", ""))
        assertTrue(errors.classDate)
        assertTrue(errors.content)
        assertTrue(errors.activity)
        assertTrue(errors.learning)
        assertFalse(errors.links)
        assertFalse(errors.isValid)
    }

    // --- AuthFormValidator ---
    @Test
    fun loginTrimsEmailBeforeValidating() {
        assertTrue(AuthFormValidator.login(" ana@escuela.cl ", "x").isValid)
    }

    @Test
    fun blankConfirmationIsAlwaysAnError() {
        val errors = AuthFormValidator.register(
            "Ana", "", "Pérez", "Soto", "123456785", "ana@escuela.cl", "clave123", ""
        )
        assertTrue(errors.confirmPassword)
        assertFalse(errors.isValid)
    }
}
