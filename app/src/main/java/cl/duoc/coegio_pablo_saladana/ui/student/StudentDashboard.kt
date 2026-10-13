package cl.duoc.coegio_pablo_saladana.ui.student

import android.net.Uri
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.UserGender
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import cl.duoc.coegio_pablo_saladana.ui.profile.ProfileAvatar

private data class MockNotebook(
    val subject: String,
    val month: String,
    val teacher: String,
    val status: String,
    val hasPendingEntry: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    fullName: String,
    rut: String,
    gender: UserGender,
    profilePhotoUri: Uri?,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotebook: () -> Unit,
    onCreateEntry: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Activas") }
    val notebooks = remember {
        listOf(
            MockNotebook(
                subject = "Lenguaje y Comunicación",
                month = "Octubre 2026",
                teacher = "Prof. Carolina Soto",
                status = "Registro pendiente",
                hasPendingEntry = true
            ),
            MockNotebook(
                subject = "Ciencias Naturales",
                month = "Octubre 2026",
                teacher = "Prof. Diego Muñoz",
                status = "Revisada",
                hasPendingEntry = false
            ),
            MockNotebook(
                subject = "Matemática",
                month = "Septiembre 2026",
                teacher = "Prof. Andrea Pérez",
                status = "Bitácora cerrada",
                hasPendingEntry = false
            )
        )
    }
    val visibleNotebooks = notebooks.filter {
        selectedFilter == "Historial" || it.status != "Bitácora cerrada"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Bitácora") },
                actions = {
                    TextButton(onClick = onOpenProfile) { Text("Perfil") }
                    TextButton(onClick = onLogout) { Text("Salir") }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedListItem(index = 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileAvatar(
                            photoUri = profilePhotoUri,
                            initials = fullName.take(1).uppercase()
                        )
                        Text(greetingFor(fullName, gender), style = MaterialTheme.typography.headlineSmall)
                        Text("RUT: $rut", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Aquí puedes revisar tus bitácoras y registrar lo aprendido en clases.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = onOpenHistory) { Text("Ver mi historial") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Activas", "Historial").forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter) }
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(
                items = visibleNotebooks,
                key = { _, notebook -> "${notebook.subject}-${notebook.month}" }
            ) { index, notebook ->
                AnimatedListItem(
                    index = index + 1,
                    modifier = Modifier.animateItem()
                ) {
                    NotebookCard(notebook, onOpenNotebook, onCreateEntry)
                }
            }
            item(key = "end-$selectedFilter") {
                EndOfContent(
                    message = if (selectedFilter == "Activas") {
                        "No hay más bitácoras activas"
                    } else {
                        "Llegaste al final del historial"
                    }
                )
            }
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
private fun NotebookCard(
    notebook: MockNotebook,
    onOpenNotebook: () -> Unit,
    onCreateEntry: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(notebook.subject, style = MaterialTheme.typography.titleLarge)
            Text(notebook.month, style = MaterialTheme.typography.labelLarge)
            Text(notebook.teacher, style = MaterialTheme.typography.bodyMedium)
            Text(
                notebook.status,
                color = if (notebook.hasPendingEntry) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.labelLarge
            )
            Button(
                onClick = if (notebook.hasPendingEntry) onCreateEntry else onOpenNotebook,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (notebook.hasPendingEntry) "Crear registro" else "Ver bitácora")
            }
        }
    }
}
