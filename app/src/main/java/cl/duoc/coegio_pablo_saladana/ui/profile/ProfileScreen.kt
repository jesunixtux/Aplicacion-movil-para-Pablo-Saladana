package cl.duoc.coegio_pablo_saladana.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.duoc.coegio_pablo_saladana.data.UserRole
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    fullName: String,
    rut: String,
    role: UserRole,
    photoUri: Uri?,
    onBack: () -> Unit,
    onPhotoSelected: (Uri?) -> Unit,
    onCameraPhotoSelected: (Bitmap?) -> Unit,
    onPhotoEdited: (Bitmap) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = onPhotoSelected
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap: Bitmap? ->
            onCameraPhotoSelected(bitmap)
        }
    )
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                cameraLauncher.launch(null)
            }
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
                initials = fullName.take(1).uppercase(),
                size = 104.dp
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
            if (photoUri != null) {
                OutlinedButton(
                    onClick = { showEditor = true },
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

    if (showEditor && photoUri != null) {
        ProfileImageEditor(
            photoUri = photoUri,
            onDismiss = { showEditor = false },
            onSave = onPhotoEdited
        )
    }
}

@Composable
private fun ProfileImageEditor(
    photoUri: Uri,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var rotation by remember(photoUri) { mutableStateOf(0f) }
    val sourceBitmap by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.contentResolver.openInputStream(photoUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
    }
    val previewBitmap = remember(sourceBitmap, rotation) {
        sourceBitmap?.let { rotateBitmap(it, rotation) }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar foto") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Vista previa de la foto editada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(220.dp)
                    )
                } else {
                    Text("No se pudo cargar la imagen")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                        Text("Girar")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    previewBitmap?.let { bitmap ->
                        onSave(cropToSquare(bitmap))
                        onDismiss()
                    }
                },
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

private fun cropToSquare(source: Bitmap): Bitmap {
    val size = minOf(source.width, source.height)
    val left = (source.width - size) / 2
    val top = (source.height - size) / 2
    return Bitmap.createBitmap(source, left, top, size, size)
}
