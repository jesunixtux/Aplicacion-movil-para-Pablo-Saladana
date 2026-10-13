package cl.duoc.coegio_pablo_saladana.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentNotebookDetail(onBack: () -> Unit, onCreateEntry: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bitácora de Lenguaje") },
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
                        Text("Octubre 2026 · 5.º Básico", style = MaterialTheme.typography.titleMedium)
                        Text("Comprender y comunicar ideas", style = MaterialTheme.typography.headlineSmall)
                        Text("Prof. Carolina Soto", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                AnimatedListItem(1) {
                    InfoCard("Propósito", "Registrar lo aprendido después de cada clase y reconocer las dificultades que necesitan apoyo.")
                }
            }
            item {
                AnimatedListItem(2) {
                    InfoCard("Instrucciones", "Completa un registro por clase. Escribe con tus propias palabras y agrega una evidencia cuando sea necesario.")
                }
            }
            item {
                AnimatedListItem(3) {
                    InfoCard("Preguntas orientadoras", "¿Qué contenido trabajamos? ¿Qué actividad realizamos? ¿Qué aprendí? ¿Qué duda me quedó?")
                }
            }
            item {
                AnimatedListItem(4) {
                    Button(onClick = onCreateEntry, modifier = Modifier.fillMaxWidth()) {
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
fun StudentEntryForm(onBack: () -> Unit, onSaved: () -> Unit) {
    var date by remember { mutableStateOf("12/10/2026") }
    var content by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var learning by remember { mutableStateOf("") }
    var doubts by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Evidencias", style = MaterialTheme.typography.titleMedium)
                        Text(attachment ?: "Aún no has agregado una evidencia.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { attachment = "Fotografía seleccionada (simulada)" }) { Text("Galería") }
                            OutlinedButton(onClick = { attachment = "Archivo seleccionado (simulado)" }) { Text("Archivo") }
                        }
                    }
                }
            }
            item {
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        if (content.isBlank() || activity.isBlank() || learning.isBlank()) {
                            message = "Completa contenido, actividad y aprendizaje."
                        } else {
                            scope.launch {
                                isLoading = true
                                delay(500)
                                isLoading = false
                                onSaved()
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { if (isLoading) CircularProgressIndicator() else Text("Enviar registro") }
            }
            item {
                OutlinedButton(
                    onClick = { message = "Borrador guardado en la maqueta." },
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
fun StudentHistory(onBack: () -> Unit) {
    val records = remember {
        listOf(
            "12/10/2026 · Lenguaje y Comunicación" to "Revisado por la docente",
            "08/10/2026 · Ciencias Naturales" to "Pendiente de revisión",
            "30/09/2026 · Matemática" to "Bitácora cerrada"
        )
    }
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
            itemsIndexed(records, key = { _, record -> record.first }) { index, record ->
                AnimatedListItem(index + 1, Modifier.animateItem()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(record.first, style = MaterialTheme.typography.titleMedium)
                            Text(record.second, color = MaterialTheme.colorScheme.primary)
                            Text("Aquí aparecerá la retroalimentación del docente cuando el registro sea revisado.")
                        }
                    }
                }
            }
            item { EndOfContent("Llegaste al final del historial") }
        }
    }
}
