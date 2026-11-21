package cl.duoc.coegio_pablo_saladana.tests

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cl.duoc.coegio_pablo_saladana.domain.auth.AuthOutcome
import cl.duoc.coegio_pablo_saladana.domain.auth.AuthProvider
import cl.duoc.coegio_pablo_saladana.domain.auth.RegistrationData
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun invalidLoginStopsBeforeProvider() {
        val provider = FakeAuthProvider()
        val model = AuthViewModel(provider)

        model.onLoginEmailChange("correo-invalido")
        model.submitLogin {}

        assertEquals(0, provider.loginCalls)
        assertTrue(model.loginUiState.isError)
        assertEquals("Revisa los campos marcados.", model.loginUiState.message)
    }

    @Test
    fun loginSuccessReturnsVerifiedAccountAndClearsForm() = runTest {
        val provider = FakeAuthProvider(loginOutcome = AuthOutcome.Success(account()))
        val model = AuthViewModel(provider)
        var loggedIn: UserAccount? = null
        model.onLoginEmailChange("alba@demo.test")
        model.onLoginPasswordChange("Demo1234")
        model.toggleLoginPasswordVisibility()

        model.submitLogin { loggedIn = it }
        advanceUntilIdle()

        assertEquals(1, provider.loginCalls)
        assertEquals(account(), loggedIn)
        assertEquals("", model.loginUiState.email)
        assertFalse(model.loginUiState.isLoading)
    }

    @Test
    fun loginFailureKeepsFormAndShowsBackendMessage() = runTest {
        val provider = FakeAuthProvider(loginOutcome = AuthOutcome.Failure("Backend apagado"))
        val model = AuthViewModel(provider)
        model.onLoginEmailChange("alba@demo.test")
        model.onLoginPasswordChange("Demo1234")

        model.submitLogin {}
        advanceUntilIdle()

        assertTrue(model.loginUiState.isError)
        assertEquals("Backend apagado", model.loginUiState.message)
        assertFalse(model.loginUiState.isLoading)
    }

    @Test
    fun registrationSanitizesFieldsAndSendsSelectedIdentity() = runTest {
        val provider = FakeAuthProvider(registerOutcome = AuthOutcome.Success(account()))
        val model = AuthViewModel(provider)
        var registered: UserAccount? = null
        model.onFirstNameChange("  Alba2 ")
        model.onFirstNameChange("Alba")
        model.onMiddleNameChange("")
        model.onPaternalSurnameChange("Demo")
        model.onMaternalSurnameChange("Prueba")
        model.onRutChange(TextFieldValue("12345678K", TextRange(9)))
        model.onRegisterEmailChange("alba@demo.test")
        model.onRegisterPasswordChange("Demo1234")
        model.onConfirmPasswordChange("Demo1234")
        model.onRoleSelected(UserRole.Student)
        model.onGenderSelected(UserGender.Female)
        model.onCourseSelected("6° Básico A")
        model.toggleRegisterPasswordVisibility()
        model.toggleConfirmPasswordVisibility()

        model.submitRegister { registered = it }
        advanceUntilIdle()

        assertEquals(account(), registered)
        assertEquals(1, provider.registerCalls)
        assertEquals("Alba", provider.registration?.firstName)
        assertEquals("6° Básico A", provider.registration?.course)
        assertEquals(UserGender.Female, provider.registration?.gender)
        assertNull(model.registerUiState.message)
    }

    @Test
    fun registrationFailureIsVisibleAndKeepsEnteredEmail() = runTest {
        val provider = FakeAuthProvider(registerOutcome = AuthOutcome.Failure("Correo ocupado"))
        val model = validRegistrationModel(provider)

        model.submitRegister {}
        advanceUntilIdle()

        assertTrue(model.registerUiState.isError)
        assertEquals("Correo ocupado", model.registerUiState.message)
        assertEquals("alba@demo.test", model.registerUiState.email)
    }

    private fun validRegistrationModel(provider: AuthProvider) = AuthViewModel(provider).apply {
        onFirstNameChange("Alba")
        onPaternalSurnameChange("Demo")
        onMaternalSurnameChange("Prueba")
        onRutChange(TextFieldValue("12345678K", TextRange(9)))
        onRegisterEmailChange("alba@demo.test")
        onRegisterPasswordChange("Demo1234")
        onConfirmPasswordChange("Demo1234")
    }

    private fun account() = UserAccount(
        firstName = "Alba", middleName = "", paternalSurname = "Demo", maternalSurname = "Prueba",
        rut = "", email = "alba@demo.test", role = UserRole.Student,
        gender = UserGender.Other, course = SchoolCourses.DEFAULT_STUDENT_COURSE, remoteId = 7
    )

    private class FakeAuthProvider(
        private val loginOutcome: AuthOutcome = AuthOutcome.Failure("No configurado"),
        private val registerOutcome: AuthOutcome = AuthOutcome.Failure("No configurado")
    ) : AuthProvider {
        override val isRemote = true
        var loginCalls = 0
        var registerCalls = 0
        var registration: RegistrationData? = null

        override suspend fun login(email: String, password: String): AuthOutcome {
            loginCalls++
            return loginOutcome
        }

        override suspend fun register(data: RegistrationData): AuthOutcome {
            registerCalls++
            registration = data
            return registerOutcome
        }

        override suspend fun restoreSession(): UserAccount? = null
        override suspend fun logout() = Unit
    }
}
