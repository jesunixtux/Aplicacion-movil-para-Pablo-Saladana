package cl.duoc.coegio_pablo_saladana.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.duoc.coegio_pablo_saladana.data.UserRole

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
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
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }
                OutlinedButton(
                    onClick = ::openCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
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
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
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
