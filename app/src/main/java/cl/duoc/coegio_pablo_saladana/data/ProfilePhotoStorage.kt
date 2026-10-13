package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

class ProfilePhotoStorage(context: Context) {
    private val resolver = context.contentResolver
    private val directory = File(context.filesDir, "profile_photos").apply { mkdirs() }

    fun uriFor(email: String): Uri? {
        return photoFile(email).takeIf(File::exists)?.let(Uri::fromFile)
    }

    fun save(email: String, source: Uri): Uri? {
        return runCatching {
            val input = resolver.openInputStream(source) ?: return null
            input.use { sourceStream ->
                photoFile(email).outputStream().use { destinationStream ->
                    sourceStream.copyTo(destinationStream)
                }
            }
            Uri.fromFile(photoFile(email))
        }.getOrNull()
    }

    fun save(email: String, bitmap: Bitmap): Uri? {
        return runCatching {
            val file = photoFile(email)
            file.outputStream().use { outputStream ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream))
            }
            Uri.fromFile(file)
        }.getOrNull()
    }

    private fun photoFile(email: String): File {
        val safeEmail = email.lowercase().replace(Regex("[^a-z0-9]"), "_")
        return File(directory, "$safeEmail.jpg")
    }
}
