package cl.duoc.coegio_pablo_saladana.ui.teacher

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import cl.duoc.coegio_pablo_saladana.ui.components.HolidayBannerCard
import cl.duoc.coegio_pablo_saladana.ui.components.greetingFor
import cl.duoc.coegio_pablo_saladana.ui.profile.ProfileAvatar
import kotlinx.coroutines.launch

/** Panel principal del docente con sus bitácoras publicadas, gestión de cursos/estudiantes y revisión de aportes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    fullName: String,
    rut: String,
    gender: UserGender,
    profilePhotoUri: Uri?,
    notebooks: List<NotebookEntity>,
    availableStudents: List<UserAccount> = emptyList(),
    submittedEntries: List<StudentEntryEntity>,
    holidays: List<ChileanHoliday>,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreateNotebook: () -> Unit,
    onUpdateNotebookAssignments: suspend (Long, String, Set<String>) -> Boolean = { _, _, _ -> false },
    onReviewEntry: suspend (Long, String) -> Boolean
) {
    val scope = rememberCoroutineScope()
    val reviewedCount = submittedEntries.count { it.isReviewed }
    val nextHoliday = holidays.firstOrNull()
    var managingNotebookId by rememberSaveable { mutableStateOf<Long?>(null) }
    val managingNotebook = notebooks.firstOrNull { it.id == managingNotebookId }

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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Text(greetingFor(fullName, gender), style = MaterialTheme.typography.headlineSmall)
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
                                }
                            }
                            Text(
                                text = "Supervisa el aprendizaje de tus cursos desde un solo lugar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                AnimatedListItem(1) {
                    MetricCard(
                        "${notebooks.size} bitácoras · $reviewedCount/${submittedEntries.size} revisados",
                        "Resumen de seguimiento docente",
                        Modifier.fillMaxWidth()
                    )
                }
            }
            if (nextHoliday != null) {
                item {
                    AnimatedListItem(2) {
                        HolidayBannerCard(
                            holiday = nextHoliday,
                            title = "Calendario escolar (API Feriados Chile)"
                        )
                    }
                }
            }
            itemsIndexed(notebooks, key = { _, notebook -> "notebook-${notebook.id}" }) { index, notebook ->
                val assignedCount = notebook.assignedStudentsSet.size
                AnimatedListItem(index + 3, Modifier.animateItem()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(10.dp).size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notebook.title, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "Publicada · Sincronizada con microservicio",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Curso: ${notebook.course}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = notebook.purpose,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (assignedCount > 0) {
                                    "$assignedCount estudiante(s) con acceso directo asignado"
                                } else {
                                    "Acceso habilitado según curso (${notebook.course})"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            OutlinedButton(
                                onClick = { managingNotebookId = notebook.id },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Gestionar curso y estudiantes")
                            }
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
                var feedbackText by rememberSaveable(entry.id) { mutableStateOf(entry.teacherFeedback) }
                var isSavingReview by remember(entry.id) { mutableStateOf(false) }

                AnimatedListItem(index + notebooks.size + 4, Modifier.animateItem()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.studentName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${entry.notebookTitle} · ${entry.classDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (entry.isReviewed) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Text(
                                        text = if (entry.isReviewed) "Estado: Revisado" else "Estado: Pendiente de revisión",
                                        color = if (entry.isReviewed) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("Contenido: ${entry.content}", style = MaterialTheme.typography.bodyMedium)
                            Text("Actividad: ${entry.activity}", style = MaterialTheme.typography.bodyMedium)
                            Text("Aprendizaje: ${entry.learning}", style = MaterialTheme.typography.bodyMedium)
                            if (entry.doubts.isNotBlank()) {
                                Text("Dudas: ${entry.doubts}", style = MaterialTheme.typography.bodyMedium)
                            }

                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { feedbackText = it },
                                label = { Text("Retroalimentación docente (opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSavingReview = true
                                        onReviewEntry(entry.id, feedbackText)
                                        isSavingReview = false
                                    }
                                },
                                enabled = !isSavingReview && (!entry.isReviewed || feedbackText.trim() != entry.teacherFeedback),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (entry.isReviewed) "Actualizar revisión" else "Marcar como revisado")
                            }
                        }
                    }
                }
            }
            item {
                AnimatedListItem(notebooks.size + 3) {
                    Button(
                        onClick = onCreateNotebook,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear nueva bitácora")
                    }
                }
            }
            item { EndOfContent("No hay más cursos por mostrar") }
        }
    }

    if (managingNotebook != null) {
        AssignStudentsDialog(
            notebook = managingNotebook,
            availableStudents = availableStudents,
            onDismiss = { managingNotebookId = null },
            onSave = { selectedCourse, selectedStudentEmails ->
                scope.launch {
                    onUpdateNotebookAssignments(
                        managingNotebook.id,
                        selectedCourse,
                        selectedStudentEmails
                    )
                    managingNotebookId = null
                }
            }
        )
    }
}

@Composable
private fun AssignStudentsDialog(
    notebook: NotebookEntity,
    availableStudents: List<UserAccount>,
    onDismiss: () -> Unit,
    onSave: (String, Set<String>) -> Unit
) {
    var selectedCourse by rememberSaveable(notebook.id) {
        mutableStateOf(notebook.course.ifBlank { SchoolCourses.GENERAL })
    }
    var selectedEmails by remember(notebook.id, notebook.assignedStudentEmails) {
        mutableStateOf(notebook.assignedStudentsSet)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Curso y estudiantes: ${notebook.title}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Curso asignado", style = MaterialTheme.typography.titleMedium)
                SchoolCourses.NOTEBOOK_COURSES.forEach { courseName ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedCourse == courseName,
                            onClick = { selectedCourse = courseName }
                        )
                        Text(courseName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Asignar acceso a cualquier estudiante", style = MaterialTheme.typography.titleMedium)
                if (availableStudents.isEmpty()) {
                    Text("No hay estudiantes registrados todavía.")
                } else {
                    availableStudents.forEach { student ->
                        val emailKey = student.email.trim().lowercase()
                        val checked = emailKey in selectedEmails
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedEmails = if (checked) {
                                        selectedEmails - emailKey
                                    } else {
                                        selectedEmails + emailKey
                                    }
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.fullName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${student.course} · ${student.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedCourse, selectedEmails) }) {
                Text("Guardar acceso")
            }
        }
    )
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
