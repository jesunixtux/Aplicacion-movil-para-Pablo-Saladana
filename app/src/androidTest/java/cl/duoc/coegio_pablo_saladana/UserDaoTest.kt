package cl.duoc.coegio_pablo_saladana

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.coegio_pablo_saladana.data.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    @Test
    fun duplicateRutIsRejectedByDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        try {
            val firstId = database.userDao().insert(testUser("first@example.com"))
            val duplicateId = database.userDao().insert(testUser("second@example.com"))

            assertTrue(firstId > 0)
            assertEquals(-1L, duplicateId)
        } finally {
            database.close()
        }
    }

    private fun testUser(email: String) = UserEntity(
        firstName = "Test",
        middleName = "",
        paternalSurname = "User",
        maternalSurname = "Example",
        rut = "23.567.224-3",
        email = email,
        role = "Student",
        gender = "Other",
        passwordHash = "test-hash"
    )
}
