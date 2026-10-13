package cl.duoc.coegio_pablo_saladana.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    fullName: String,
    rut: String,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreateNotebook: () -> Unit,
    onReviewEntries: () -> Unit
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
                Text("Hola, $fullName", style = MaterialTheme.typography.headlineSmall)
                Text("RUT: $rut", style = MaterialTheme.typography.labelLarge)
                Text("Supervisa el aprendizaje de tus cursos desde un solo lugar.")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("2", "Bitácoras activas", Modifier.weight(1f))
                    MetricCard("24", "Participantes", Modifier.weight(1f))
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lenguaje y Comunicación", style = MaterialTheme.typography.titleLarge)
                        Text("5.º Básico · Octubre 2026")
                        Text("18 de 24 estudiantes han participado", color = MaterialTheme.colorScheme.primary)
                        Button(onClick = onReviewEntries, modifier = Modifier.fillMaxWidth()) { Text("Revisar aportes") }
                    }
                }
            }
            item { Button(onClick = onCreateNotebook, modifier = Modifier.fillMaxWidth()) { Text("Crear nueva bitácora") } }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCreateNotebook(onBack: () -> Unit, onPublished: () -> Unit) {
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
                        if (title.isBlank() || purpose.isBlank() || instructions.isBlank()) {
                            message = "Completa título, propósito e instrucciones."
                        } else {
                            scope.launch {
                                loading = true
                                delay(500)
                                loading = false
                                onPublished()
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { if (loading) CircularProgressIndicator() else Text("Publicar bitácora") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherReviewEntries(onBack: () -> Unit) {
    var reviewed by remember { mutableStateOf(setOf<String>()) }
    val entries = listOf("Sofía Martínez" to "Pendiente", "Tomás Rojas" to "Pendiente", "Valentina Pérez" to "Revisado")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revisar aportes") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Lenguaje y Comunicación · Octubre 2026", style = MaterialTheme.typography.titleMedium) }
            item { Text("18 participantes · 6 aportes pendientes", color = MaterialTheme.colorScheme.primary) }
            items(entries) { entry ->
                val name = entry.first
                val isReviewed = name in reviewed || entry.second == "Revisado"
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text("Contenido trabajado y aprendizaje registrado por el estudiante.")
                        Text(if (isReviewed) "Revisado" else "Pendiente de revisión", color = MaterialTheme.colorScheme.primary)
                        if (!isReviewed) {
                            OutlinedButton(onClick = { reviewed = reviewed + name }) { Text("Marcar como revisado") }
                        }
                    }
                }
            }
        }
    }
}
