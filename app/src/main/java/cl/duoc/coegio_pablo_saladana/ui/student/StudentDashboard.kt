package cl.duoc.coegio_pablo_saladana.ui.student

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import cl.duoc.coegio_pablo_saladana.ui.components.HolidayBannerCard
import cl.duoc.coegio_pablo_saladana.ui.components.greetingFor
import cl.duoc.coegio_pablo_saladana.ui.profile.ProfileAvatar

/** Panel principal del estudiante con las bitácoras disponibles para su curso o asignación directa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    fullName: String,
    rut: String,
    course: String = SchoolCourses.DEFAULT_STUDENT_COURSE,
    gender: UserGender,
    profilePhotoUri: Uri?,
    notebooks: List<NotebookEntity>,
    entries: List<StudentEntryEntity> = emptyList(),
    holidays: List<ChileanHoliday>,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotebook: (NotebookEntity) -> Unit,
    onOpenHistory: () -> Unit
) {
    val nextHoliday = holidays.firstOrNull()
    val pendingNotebooks = notebooks.filter { notebook ->
        notebook.isOpen && entries.none { entry -> entry.notebookId == notebook.id && !entry.isDraft }
    }
    val draftCount = entries.count { it.isDraft }
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
                    StudentHeaderCard(
                        fullName = fullName,
                        rut = rut,
                        course = course,
                        gender = gender,
                        profilePhotoUri = profilePhotoUri,
                        onOpenHistory = onOpenHistory
                    )
                }
            }

            if (nextHoliday != null) {
                item(key = "holiday-banner") {
                    AnimatedListItem(index = 1) {
                        HolidayBannerCard(holiday = nextHoliday)
                    }
                }
            }

            if (pendingNotebooks.isNotEmpty()) {
                item {
                    Text(
                        "Aviso: tienes ${pendingNotebooks.size} bitácora(s) activa(s) sin aporte enviado.",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (draftCount > 0) {
                item { Text("Tienes $draftCount borrador(es) guardado(s) en tu historial.") }
            }

            itemsIndexed(
                items = notebooks,
                key = { _, notebook -> notebook.id }
            ) { index, notebook ->
                AnimatedListItem(
                    index = index + 2,
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

@Composable
private fun StudentHeaderCard(
    fullName: String,
    rut: String,
    course: String,
    gender: UserGender,
    profilePhotoUri: Uri?,
    onOpenHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileAvatar(
                    photoUri = profilePhotoUri,
                    initials = fullName.take(1).uppercase()
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = greetingFor(fullName, gender),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "RUT: $rut",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Curso: $course",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = "Aquí puedes revisar tus bitácoras y registrar lo aprendido en clases.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onOpenHistory,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver mi historial")
            }
        }
    }
}

@Composable
private fun NotebookCard(
    notebook: NotebookEntity,
    onOpenNotebook: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = notebook.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                            text = "Docente: ${notebook.teacherName} · ${notebook.course} · ${notebook.subject} · ${notebook.periodLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (notebook.isClosed) "Cerrada" else "Disponible",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (notebook.purpose.isNotBlank()) {
                Text(
                    text = notebook.purpose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Button(
                onClick = onOpenNotebook,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver bitácora")
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
