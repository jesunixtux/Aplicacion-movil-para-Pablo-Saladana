package cl.duoc.coegio_pablo_saladana.ui.student

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
import androidx.compose.material3.OutlinedButton
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
import cl.duoc.coegio_pablo_saladana.data.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.data.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentNotebookDetail(
    notebook: NotebookEntity?,
    onBack: () -> Unit,
    onCreateEntry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(notebook?.title ?: "Bitácora") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedListItem(0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            notebook?.title ?: "Bitácora no disponible",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        notebook?.let {
                            Text("Docente: ${it.teacherName}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                AnimatedListItem(1) {
                    InfoCard("Propósito", notebook?.purpose ?: "No disponible")
                }
            }
            item {
                AnimatedListItem(2) {
                    InfoCard("Instrucciones", notebook?.instructions ?: "No disponible")
                }
            }
            item {
                AnimatedListItem(3) {
                    InfoCard(
                        "Preguntas orientadoras",
                        notebook?.questions?.ifBlank { "Sin preguntas orientadoras." } ?: "No disponible"
                    )
                }
            }
            item {
                AnimatedListItem(4) {
                    Button(
                        onClick = onCreateEntry,
                        enabled = notebook != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear registro de clase")
                    }
                }
            }
            item {
                EndOfContent("No hay más información en esta bitácora")
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEntryForm(
    onBack: () -> Unit,
    onSave: suspend (StudentEntryDraft, Boolean) -> Boolean,
    onSaved: () -> Unit
) {
    var date by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var content by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var learning by remember { mutableStateOf("") }
    var doubts by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val saveEntry: (Boolean) -> Unit = { isDraft ->
        val draft = StudentEntryDraft(date, content, activity, learning, doubts)
        if ((!isDraft && !draft.isComplete()) || (isDraft && !draft.hasContent())) {
            message = if (isDraft) {
                "Escribe al menos un dato antes de guardar el borrador."
            } else if (!draft.hasValidClassDate()) {
                "Ingresa una fecha válida con formato dd/MM/aaaa."
            } else {
                "Completa fecha, contenido, actividad y aprendizaje."
            }
        } else {
            scope.launch {
                isLoading = true
                val saved = onSave(draft, isDraft)
                isLoading = false
                if (saved) onSaved() else message = "No se pudo guardar el registro. Inténtalo nuevamente."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo registro") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Registro de clase", style = MaterialTheme.typography.headlineSmall) }
            item { OutlinedTextField(date, { date = it }, label = { Text("Fecha de la clase") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(content, { content = it; message = null }, label = { Text("Contenido trabajado") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(activity, { activity = it; message = null }, label = { Text("Actividad realizada") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(learning, { learning = it; message = null }, label = { Text("¿Qué aprendí?") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(doubts, { doubts = it }, label = { Text("Dudas o dificultades") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item {
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { saveEntry(false) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { if (isLoading) CircularProgressIndicator() else Text("Enviar registro") }
            }
            item {
                OutlinedButton(
                    onClick = { saveEntry(true) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar como borrador") }
            }
            item { EndOfContent("Fin del formulario") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHistory(entries: List<StudentEntryEntity>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi historial") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Tus registros", style = MaterialTheme.typography.headlineSmall) }
            if (entries.isEmpty()) {
                item { Text("Aún no has creado registros.") }
            }
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                AnimatedListItem(index + 1, Modifier.animateItem()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${entry.classDate} · ${entry.notebookTitle}", style = MaterialTheme.typography.titleMedium)
                            Text(if (entry.isDraft) "Borrador" else "Enviado", color = MaterialTheme.colorScheme.primary)
                            Text(entry.learning.ifBlank { entry.content })
                        }
                    }
                }
            }
            item { EndOfContent("Llegaste al final del historial") }
        }
    }
}
