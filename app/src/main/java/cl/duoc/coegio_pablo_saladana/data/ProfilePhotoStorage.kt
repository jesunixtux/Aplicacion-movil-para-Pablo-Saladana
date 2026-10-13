package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

class ProfilePhotoStorage(context: Context) {
    private val directory = File(context.filesDir, "profile_photos").apply { mkdirs() }

    fun uriFor(email: String): Uri? {
        return photoFile(email).takeIf(File::exists)?.also(::normalizeExisting)?.let(::versionedUri)
    }

    fun save(email: String, bitmap: Bitmap): Uri? {
        return runCatching {
            val file = photoFile(email)
            val normalizedBitmap = normalize(bitmap)
            file.outputStream().use { outputStream ->
                check(normalizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream))
            }
            versionedUri(file, System.nanoTime().toString())
        }.getOrNull()
    }

    private fun normalize(source: Bitmap): Bitmap {
        val cropSize = minOf(source.width, source.height)
        val left = (source.width - cropSize) / 2
        val top = (source.height - cropSize) / 2
        val square = Bitmap.createBitmap(source, left, top, cropSize, cropSize)
        return if (square.width == PHOTO_SIZE_PIXELS && square.height == PHOTO_SIZE_PIXELS) {
            square
        } else {
            Bitmap.createScaledBitmap(square, PHOTO_SIZE_PIXELS, PHOTO_SIZE_PIXELS, true)
        }
    }

    private fun normalizeExisting(file: File) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth == PHOTO_SIZE_PIXELS && bounds.outHeight == PHOTO_SIZE_PIXELS) return
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return

            var sampleSize = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= PHOTO_SIZE_PIXELS) {
                sampleSize *= 2
            }
            val source = BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            ) ?: return
            val normalized = normalize(source)
            file.outputStream().use { outputStream ->
                check(normalized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream))
            }
        }
    }

    private fun versionedUri(file: File, version: String = file.lastModified().toString()): Uri {
        return Uri.fromFile(file).buildUpon().fragment(version).build()
    }

    private fun photoFile(email: String): File {
        val safeEmail = email.lowercase().replace(Regex("[^a-z0-9]"), "_")
        return File(directory, "$safeEmail.jpg")
    }

    companion object {
        const val PHOTO_SIZE_PIXELS = 64
        private const val JPEG_QUALITY = 90
    }
}
