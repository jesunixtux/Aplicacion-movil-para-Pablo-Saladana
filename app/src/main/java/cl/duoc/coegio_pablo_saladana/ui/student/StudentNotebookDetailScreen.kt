package cl.duoc.coegio_pablo_saladana.ui.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent

/** Detalle de una bitácora seleccionada antes de crear un registro de clase. */
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
                            Text(
                                text = "${it.course} · ${it.subject} · ${it.periodLabel} · " +
                                    (if (it.isClosed) "Cerrada" else "Publicada") + " · Docente: ${it.teacherName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        enabled = notebook?.isOpen == true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear registro de clase")
                    }
                    if (notebook?.isClosed == true) Text("Bitácora cerrada: puedes consultar tu historial, pero no enviar más registros.")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
