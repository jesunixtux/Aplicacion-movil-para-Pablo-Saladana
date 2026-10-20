package cl.duoc.coegio_pablo_saladana.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.core.content.ContextCompat
import cl.duoc.coegio_pablo_saladana.data.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Presenta los datos de la cuenta y permite cargar, capturar y ajustar su foto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    fullName: String,
    rut: String,
    role: UserRole,
    photoUri: Uri?,
    onBack: () -> Unit,
    onPhotoEdited: (Bitmap) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var editorUri by remember { mutableStateOf<Uri?>(null) }
    var editorBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun openEditor(uri: Uri? = null, bitmap: Bitmap? = null) {
        editorUri = uri
        editorBitmap = bitmap
    }

    fun closeEditor() {
        editorUri = null
        editorBitmap = null
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { selectedUri ->
            if (selectedUri != null) openEditor(uri = selectedUri)
        }
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) openEditor(bitmap = bitmap)
        }
    )
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) cameraLauncher.launch(null)
        }
    )

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Información de la cuenta", style = MaterialTheme.typography.headlineSmall)
            ProfileAvatar(
                photoUri = photoUri,
                initials = fullName.take(1).uppercase()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Galería")
                }
                OutlinedButton(
                    onClick = ::openCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cámara")
                }
            }
            AnimatedVisibility(
                visible = photoUri != null,
                enter = fadeIn(tween(ProfilePhotoDefaults.AnimationDurationMillis)) +
                    expandVertically() + scaleIn(initialScale = 0.96f),
                exit = fadeOut(tween(ProfilePhotoDefaults.AnimationDurationMillis / 2)) +
                    shrinkVertically()
            ) {
                OutlinedButton(
                    onClick = { photoUri?.let { openEditor(uri = it) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar foto")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nombre completo", style = MaterialTheme.typography.labelLarge)
                    Text(fullName, style = MaterialTheme.typography.titleLarge)
                    Text("RUT: $rut")
                    Text("Perfil: ${if (role == UserRole.Student) "Estudiante" else "Docente"}")
                }
            }
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        }
    }

    if (editorUri != null || editorBitmap != null) {
        ProfileImageEditor(
            sourceUri = editorUri,
            sourceBitmap = editorBitmap,
            onDismiss = ::closeEditor,
            onSave = { bitmap ->
                onPhotoEdited(bitmap)
                closeEditor()
            }
        )
    }
}

/** Editor simple que recorta, desplaza y rota una imagen antes de guardarla. */
@Composable
private fun ProfileImageEditor(
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

    androidx.compose.material3.AlertDialog(
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
                    androidx.compose.animation.Crossfade(
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
