package cl.duoc.coegio_pablo_saladana.ui.profile

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Editor simple que recorta, desplaza y rota una imagen antes de guardarla. */
@Composable
internal fun ProfileImageEditor(
    sourceUri: Uri?,
    sourceBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var rotation by remember(sourceUri, sourceBitmap) { mutableIntStateOf(0) }
    var zoom by remember(sourceUri, sourceBitmap) { mutableFloatStateOf(1f) }
    var horizontalPosition by remember(sourceUri, sourceBitmap) { mutableFloatStateOf(0f) }
    var verticalPosition by remember(sourceUri, sourceBitmap) { mutableFloatStateOf(0f) }
    val loadedBitmap by produceState<Bitmap?>(
        initialValue = sourceBitmap,
        key1 = sourceUri,
        key2 = sourceBitmap
    ) {
        value = sourceBitmap ?: sourceUri?.let { uri ->
            withContext(Dispatchers.IO) {
                decodeSampledBitmap(context, uri, ProfilePhotoDefaults.EditorMaxImagePixels)
            }
        }
    }
    val rotatedBitmap = remember(loadedBitmap, rotation) {
        loadedBitmap?.let { rotateBitmap(it, rotation.toFloat()) }
    }
    val previewBitmap = remember(
        rotatedBitmap,
        zoom,
        horizontalPosition,
        verticalPosition
    ) {
        rotatedBitmap?.let {
            cropToSquare(it, zoom, horizontalPosition, verticalPosition)
        }
    }
    val cropSize = rotatedBitmap?.let { (minOf(it.width, it.height) / zoom).roundToInt() } ?: 0
    val canMoveHorizontally = rotatedBitmap?.width?.let { it > cropSize } == true
    val canMoveVertically = rotatedBitmap?.height?.let { it > cropSize } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar foto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(ProfilePhotoDefaults.EditorPreviewSize)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = previewBitmap,
                        animationSpec = tween(ProfilePhotoDefaults.AnimationDurationMillis),
                        label = "profile_crop_preview"
                    ) { bitmap ->
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Vista previa del recorte",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Cargando imagen…")
                        }
                    }
                }

                Text("Zoom ${"%.1f".format(zoom)}×", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 1f..ProfilePhotoDefaults.MaxZoom
                )
                Text("Posición horizontal", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = horizontalPosition,
                    onValueChange = { horizontalPosition = it },
                    valueRange = -1f..1f,
                    enabled = canMoveHorizontally
                )
                Text("Posición vertical", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = verticalPosition,
                    onValueChange = { verticalPosition = it },
                    valueRange = -1f..1f,
                    enabled = canMoveVertically
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            rotation = (rotation + 90) % 360
                            horizontalPosition = 0f
                            verticalPosition = 0f
                        }
                    ) {
                        Text("Girar")
                    }
                    TextButton(
                        onClick = {
                            zoom = 1f
                            horizontalPosition = 0f
                            verticalPosition = 0f
                        }
                    ) {
                        Text("Centrar")
                    }
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { previewBitmap?.let(onSave) },
                enabled = previewBitmap != null
            ) {
                Text("Guardar")
            }
        }
    )
}

private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return source
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun cropToSquare(
    source: Bitmap,
    zoom: Float,
    horizontalPosition: Float,
    verticalPosition: Float
): Bitmap {
    val cropSize = (minOf(source.width, source.height) /
        zoom.coerceIn(1f, ProfilePhotoDefaults.MaxZoom))
        .roundToInt()
        .coerceAtLeast(1)
    val maxLeft = (source.width - cropSize).coerceAtLeast(0)
    val maxTop = (source.height - cropSize).coerceAtLeast(0)
    val left = (((horizontalPosition.coerceIn(-1f, 1f) + 1f) / 2f) * maxLeft)
        .roundToInt()
        .coerceIn(0, maxLeft)
    val top = (((verticalPosition.coerceIn(-1f, 1f) + 1f) / 2f) * maxTop)
        .roundToInt()
        .coerceIn(0, maxTop)
    return Bitmap.createBitmap(source, left, top, cropSize, cropSize)
}
