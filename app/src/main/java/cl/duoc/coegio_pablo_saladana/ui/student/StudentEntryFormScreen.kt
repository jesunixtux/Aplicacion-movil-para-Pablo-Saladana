package cl.duoc.coegio_pablo_saladana.ui.student

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.HolidayRemoteDataSource
import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent

/** Formulario para guardar un borrador o enviar un registro completo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEntryForm(
    initialEntry: StudentEntryEntity? = null,
    notebookPeriod: String? = null,
    holidays: List<ChileanHoliday> = emptyList(),
    onBack: () -> Unit,
    onSave: suspend (StudentEntryDraft, Boolean) -> Boolean,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember(context) {
        (context as? ViewModelStoreOwner)?.let {
            ViewModelProvider(it)[StudentEntryViewModel::class.java]
        } ?: StudentEntryViewModel(initialEntry)
    }
    LaunchedEffect(initialEntry?.id) {
        viewModel.bindEntry(initialEntry)
    }
    val state = viewModel.uiState
    var attachmentError by rememberSaveable { mutableStateOf<String?>(null) }
    val resolver = context.contentResolver
    fun savePickedUri(uri: android.net.Uri, isPhoto: Boolean) {
        val bytes = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        }.getOrNull()
        if (bytes == null || bytes > 10L * 1024L * 1024L) {
            attachmentError = "Solo se permiten archivos de hasta 10 MB con tamaño conocido."
            return
        }
        val persisted = runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        if (!persisted) {
            attachmentError = "No se pudo conservar el permiso de lectura del archivo."
            return
        }
        attachmentError = null
        if (isPhoto) viewModel.onPhotoAdded(uri.toString()) else viewModel.onFileAdded(uri.toString())
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) savePickedUri(uri, isPhoto = true)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) savePickedUri(uri, isPhoto = false)
    }
    val matchedHoliday = HolidayRemoteDataSource.findHolidayForClassDate(state.date, holidays)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialEntry != null) "Editar borrador" else "Nuevo registro") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Registro de clase", style = MaterialTheme.typography.headlineSmall)
                notebookPeriod?.let { Text("La fecha de la clase debe corresponder al periodo $it.") }
            }
            item {
                OutlinedTextField(
                    value = state.date,
                    onValueChange = viewModel::onDateChange,
                    label = { Text("Fecha de la clase") },
                    isError = state.attemptedSubmit && state.errors.classDate,
                    supportingText = {
                        if (state.attemptedSubmit && state.errors.classDate) {
                            Text("Ingresa una fecha válida (dd/MM/aaaa).")
                        } else if (matchedHoliday != null) {
                            Text(
                                "Aviso API Feriados Chile: ${matchedHoliday.localName}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Evidencias de la clase", style = MaterialTheme.typography.titleMedium)
                Text("Puedes añadir fotos, archivos de hasta 10 MB y enlaces HTTPS.")
                OutlinedButton(
                    onClick = { photoPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Adjuntar fotografía") }
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("application/pdf", "text/plain", "image/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Adjuntar archivo") }
                state.photoUris.lineSequence().filter(String::isNotBlank).forEachIndexed { index, uri ->
                    TextButton(onClick = { viewModel.onRemovePhoto(uri) }) {
                        Text("Foto ${index + 1} adjunta · Quitar")
                    }
                }
                state.fileUris.lineSequence().filter(String::isNotBlank).forEachIndexed { index, uri ->
                    TextButton(onClick = { viewModel.onRemoveFile(uri) }) {
                        Text("Archivo ${index + 1} adjunto · Quitar")
                    }
                }
                OutlinedTextField(
                    value = state.links,
                    onValueChange = viewModel::onLinksChange,
                    label = { Text("Enlaces (uno por línea)") },
                    minLines = 2,
                    isError = state.attemptedSubmit && state.errors.links,
                    supportingText = {
                        if (state.attemptedSubmit && state.errors.links) Text("Usa enlaces http:// o https:// válidos.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                attachmentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item {
                OutlinedTextField(
                    value = state.content,
                    onValueChange = viewModel::onContentChange,
                    label = { Text("Contenido trabajado") },
                    minLines = 3,
                    isError = state.attemptedSubmit && state.errors.content,
                    supportingText = {
                        if (state.attemptedSubmit && state.errors.content) Text("Ingresa el contenido trabajado.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.activity,
                    onValueChange = viewModel::onActivityChange,
                    label = { Text("Actividad realizada") },
                    minLines = 3,
                    isError = state.attemptedSubmit && state.errors.activity,
                    supportingText = {
                        if (state.attemptedSubmit && state.errors.activity) Text("Ingresa la actividad realizada.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.learning,
                    onValueChange = viewModel::onLearningChange,
                    label = { Text("¿Qué aprendí?") },
                    minLines = 3,
                    isError = state.attemptedSubmit && state.errors.learning,
                    supportingText = {
                        if (state.attemptedSubmit && state.errors.learning) Text("Describe lo que aprendiste.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.doubts,
                    onValueChange = viewModel::onDoubtsChange,
                    label = { Text("Dudas o dificultades") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { viewModel.saveEntry(isDraft = false, onSave = onSave, onSaved = onSaved) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) CircularProgressIndicator() else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar registro")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.saveEntry(isDraft = true, onSave = onSave, onSaved = onSaved) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar como borrador")
                }
            }
            item { EndOfContent("Fin del formulario") }
        }
    }
}
