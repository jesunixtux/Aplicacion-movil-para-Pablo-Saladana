package cl.duoc.coegio_pablo_saladana.ui.teacher

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.NotebookDraft
import cl.duoc.coegio_pablo_saladana.data.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.UserGender
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import cl.duoc.coegio_pablo_saladana.ui.profile.ProfileAvatar
import kotlinx.coroutines.launch

/** Panel principal del docente con sus bitácoras publicadas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    fullName: String,
    rut: String,
    gender: UserGender,
    profilePhotoUri: Uri?,
    notebooks: List<NotebookEntity>,
    submittedEntries: List<StudentEntryEntity>,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreateNotebook: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel docente") },
                actions = {
                    TextButton(onClick = onOpenProfile) { Text("Perfil") }
                    TextButton(onClick = onLogout) { Text("Salir") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedListItem(0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProfileAvatar(
                            photoUri = profilePhotoUri,
                            initials = fullName.take(1).uppercase()
                        )
                        Text(greetingFor(fullName, gender), style = MaterialTheme.typography.headlineSmall)
                        Text("RUT: $rut", style = MaterialTheme.typography.labelLarge)
                        Text("Supervisa el aprendizaje de tus cursos desde un solo lugar.")
                    }
                }
            }
            item {
                AnimatedListItem(1) {
                    MetricCard(notebooks.size.toString(), "Bitácoras publicadas", Modifier.fillMaxWidth())
                }
            }
            itemsIndexed(notebooks, key = { _, notebook -> "notebook-${notebook.id}" }) { index, notebook ->
                AnimatedListItem(index + 2, Modifier.animateItem()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(notebook.title, style = MaterialTheme.typography.titleLarge)
                            Text(notebook.purpose)
                            Text("Publicada", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item {
                Text("Registros enviados", style = MaterialTheme.typography.titleLarge)
            }
            if (submittedEntries.isEmpty()) {
                item { Text("Aún no hay registros enviados por estudiantes.") }
            }
            itemsIndexed(submittedEntries, key = { _, entry -> "entry-${entry.id}" }) { index, entry ->
                AnimatedListItem(index + notebooks.size + 3, Modifier.animateItem()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.studentName, style = MaterialTheme.typography.titleMedium)
                            Text("${entry.notebookTitle} · ${entry.classDate}")
                            Text("Contenido: ${entry.content}")
                            Text("Actividad: ${entry.activity}")
                            Text("Aprendizaje: ${entry.learning}")
                            if (entry.doubts.isNotBlank()) Text("Dudas: ${entry.doubts}")
                        }
                    }
                }
            }
            item {
                AnimatedListItem(notebooks.size + 2) {
                    Button(onClick = onCreateNotebook, modifier = Modifier.fillMaxWidth()) {
                        Text("Crear nueva bitácora")
                    }
                }
            }
            item { EndOfContent("No hay más cursos por mostrar") }
        }
    }
}
private fun greetingFor(fullName: String, gender: UserGender): String {
    return when (gender) {
        UserGender.Male -> "Hola Sr. $fullName"
        UserGender.Female -> "Hola Sra. $fullName"
        UserGender.Other -> "Hola, $fullName"
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Formulario para crear y publicar una bitácora mensual. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCreateNotebook(
    onBack: () -> Unit,
    onPublish: suspend (NotebookDraft) -> Boolean,
    onPublished: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva bitácora") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Configura la bitácora mensual", style = MaterialTheme.typography.headlineSmall) }
            item { OutlinedTextField(title, { title = it; message = null }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(purpose, { purpose = it; message = null }, label = { Text("Propósito") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(instructions, { instructions = it; message = null }, label = { Text("Instrucciones") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(questions, { questions = it; message = null }, label = { Text("Preguntas orientadoras") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item {
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        val draft = NotebookDraft(title, purpose, instructions, questions)
                        if (!draft.isValid()) {
                            message = "Completa título, propósito e instrucciones."
                        } else {
                            scope.launch {
                                loading = true
                                val published = onPublish(draft)
                                loading = false
                                if (published) {
                                    onPublished()
                                } else {
                                    message = "No se pudo publicar la bitácora. Inténtalo nuevamente."
                                }
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { if (loading) CircularProgressIndicator() else Text("Publicar bitácora") }
            }
            item { EndOfContent("Fin del formulario") }
        }
    }
}
