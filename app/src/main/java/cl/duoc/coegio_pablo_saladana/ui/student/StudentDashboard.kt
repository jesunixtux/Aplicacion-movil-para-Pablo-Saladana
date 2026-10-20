package cl.duoc.coegio_pablo_saladana.ui.student

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.UserGender
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import cl.duoc.coegio_pablo_saladana.ui.profile.ProfileAvatar

/** Panel principal del estudiante con las bitácoras disponibles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    fullName: String,
    rut: String,
    gender: UserGender,
    profilePhotoUri: Uri?,
    notebooks: List<NotebookEntity>,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotebook: (NotebookEntity) -> Unit,
    onOpenHistory: () -> Unit
) {
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
                    }
                }
            }

            itemsIndexed(
                items = notebooks,
                key = { _, notebook -> notebook.id }
            ) { index, notebook ->
                AnimatedListItem(
                    index = index + 1,
                    modifier = Modifier.animateItem()
                ) {
                    NotebookCard(notebook) { onOpenNotebook(notebook) }
                }
            }
            item(key = "end") {
                EndOfContent(if (notebooks.isEmpty()) "Aún no hay bitácoras publicadas" else "No hay más bitácoras")
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
    notebook: NotebookEntity,
    onOpenNotebook: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(notebook.title, style = MaterialTheme.typography.titleLarge)
            Text("Docente: ${notebook.teacherName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Disponible",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Button(
                onClick = onOpenNotebook,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver bitácora")
            }
        }
    }
}
