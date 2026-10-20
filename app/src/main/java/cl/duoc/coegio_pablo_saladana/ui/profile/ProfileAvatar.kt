package cl.duoc.coegio_pablo_saladana.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Valores compartidos de tamaño, recorte y transición para fotos de perfil. */
object ProfilePhotoDefaults {
    val AvatarSize = 64.dp
    val EditorPreviewSize = 200.dp
    const val AnimationDurationMillis = 320
    const val AvatarDecodeMaxImagePixels = 128
    const val EditorMaxImagePixels = 1024
    const val MaxZoom = 3f
}

/** Dibuja una foto guardada o, si no existe, las iniciales de la cuenta. */
@Composable
fun ProfileAvatar(
    photoUri: Uri?,
    initials: String,
    size: Dp = ProfilePhotoDefaults.AvatarSize
) {
    val context = LocalContext.current
    val loadedBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = photoUri) {
        value = photoUri?.let { uri ->
            withContext(Dispatchers.IO) {
                decodeSampledBitmap(
                    context,
                    uri,
                    ProfilePhotoDefaults.AvatarDecodeMaxImagePixels
                )?.asImageBitmap()
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = loadedBitmap,
            transitionSpec = {
                (fadeIn(tween(ProfilePhotoDefaults.AnimationDurationMillis)) +
                    scaleIn(initialScale = 0.88f)) togetherWith
                    (fadeOut(tween(ProfilePhotoDefaults.AnimationDurationMillis / 2)) +
                        scaleOut(targetScale = 1.06f))
            },
            label = "profile_photo",
            modifier = Modifier.fillMaxSize()
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = initials.ifBlank { "U" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

internal fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimension ||
        bounds.outHeight / sampleSize > maxDimension
    ) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}
