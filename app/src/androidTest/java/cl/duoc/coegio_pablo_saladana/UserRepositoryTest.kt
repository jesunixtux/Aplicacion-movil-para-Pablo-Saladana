package cl.duoc.coegio_pablo_saladana

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.coegio_pablo_saladana.data.UserGender
import cl.duoc.coegio_pablo_saladana.data.UserRepository
import cl.duoc.coegio_pablo_saladana.data.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserRepositoryTest {
    @Test
    fun registerRejectsInvalidDataBeforeWriting() = runBlocking {
        val repository = UserRepository(InstrumentationRegistry.getInstrumentation().targetContext)

        assertNull(register(repository, firstName = "Ana", email = "ana@", password = "Clave123"))
        assertNull(register(repository, firstName = "Ana2", email = "ana@example.com", password = "Clave123"))
        assertNull(register(repository, firstName = "Ana", email = "ana@example.com", password = "corta"))
    }

    private suspend fun register(repository: UserRepository, firstName: String, email: String, password: String) =
        repository.register(
            firstName = firstName,
            middleName = "",
            paternalSurname = "Pérez",
            maternalSurname = "Soto",
            rut = "12.345.678-5",
            email = email,
            password = password,
            role = UserRole.Student,
            gender = UserGender.Other
        )
}
