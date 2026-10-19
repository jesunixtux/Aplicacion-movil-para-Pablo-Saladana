package cl.duoc.coegio_pablo_saladana

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.coegio_pablo_saladana.data.ProfilePhotoStorage
import java.io.File
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePhotoStorageTest {
    @Test
    fun storesPreviouslyCollidingEmailsInDifferentFiles() {
        val storage = ProfilePhotoStorage(InstrumentationRegistry.getInstrumentation().targetContext)
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        var firstPath: String? = null
        var secondPath: String? = null

        try {
            firstPath = storage.save("alumno.a@escuela.cl", bitmap)?.path
            secondPath = storage.save("alumno_a@escuela.cl", bitmap)?.path

            assertNotNull(firstPath)
            assertNotNull(secondPath)
            assertNotEquals(firstPath, secondPath)
        } finally {
            listOfNotNull(firstPath, secondPath).distinct().forEach { File(it).delete() }
            bitmap.recycle()
        }
    }
}
