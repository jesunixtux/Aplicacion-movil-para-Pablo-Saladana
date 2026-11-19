package cl.duoc.coegio_pablo_saladana.ui.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent

/** Formulario para crear y publicar una bitácora mensual, asignando curso y estudiantes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCreateNotebook(
    availableStudents: List<UserAccount> = emptyList(),
    teacherCourse: String?,
    teacherSubject: String?,
    onBack: () -> Unit,
    onPublish: suspend (NotebookDraft) -> String?,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember(context) {
        (context as? ViewModelStoreOwner)?.let {
            ViewModelProvider(it)[TeacherNotebookViewModel::class.java]
        } ?: TeacherNotebookViewModel()
    }
    val state = viewModel.uiState
    val filteredStudents = if (teacherCourse != null) {
        availableStudents.filter { it.course == teacherCourse }
    } else {
        availableStudents
    }

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
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.monthText,
                        onValueChange = viewModel::onMonthChange,
                        label = { Text("Mes (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.attemptedPublish && state.errors.period,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.yearText,
                        onValueChange = viewModel::onYearChange,
                        label = { Text("Año") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.attemptedPublish && state.errors.period,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                if (teacherSubject != null) {
                    Text("Asignatura asignada: $teacherSubject", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Asignatura", style = MaterialTheme.typography.titleMedium)
                    SchoolSubjects.ALL.forEach { subject ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.subject == subject, onClick = { viewModel.onSubjectChange(subject) })
                            Text(subject)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Título") },
                    isError = state.attemptedPublish && state.errors.title,
                    supportingText = {
                        if (state.attemptedPublish && state.errors.title) Text("Ingresa un título.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.purpose,
                    onValueChange = viewModel::onPurposeChange,
                    label = { Text("Propósito") },
                    minLines = 3,
                    isError = state.attemptedPublish && state.errors.purpose,
                    supportingText = {
                        if (state.attemptedPublish && state.errors.purpose) Text("Ingresa el propósito.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.instructions,
                    onValueChange = viewModel::onInstructionsChange,
                    label = { Text("Instrucciones") },
                    minLines = 3,
                    isError = state.attemptedPublish && state.errors.instructions,
                    supportingText = {
                        if (state.attemptedPublish && state.errors.instructions) Text("Ingresa las instrucciones.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.questions,
                    onValueChange = viewModel::onQuestionsChange,
                    label = { Text("Preguntas orientadoras") },
                    minLines = 3,
                    isError = state.attemptedPublish && state.errors.questions,
                    supportingText = { if (state.attemptedPublish && state.errors.questions) Text("Ingresa al menos una pregunta.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Curso de la bitácora", style = MaterialTheme.typography.titleMedium)
                        if (teacherCourse != null) {
                            Text(
                                text = "Curso asignado: $teacherCourse",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "La bitácora se creará automáticamente para tu curso asignado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Selecciona uno de los cursos del caso (5.º a 8.º básico).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SchoolCourses.NOTEBOOK_COURSES.forEach { courseName ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = state.course == courseName,
                                        onClick = { viewModel.onCourseChange(courseName) }
                                    )
                                    Text(courseName, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (teacherCourse != null) "Estudiantes de $teacherCourse" else "Agregar estudiantes a la bitácora",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (teacherCourse != null)
                                "Solo puedes asignar estudiantes de tu curso."
                            else
                                "Solo se otorgará acceso a estudiantes del curso seleccionado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filteredStudents.isEmpty()) {
                            Text(
                                text = if (teacherCourse != null)
                                    "Aún no hay estudiantes registrados en tu curso."
                                else
                                    "Aún no hay usuarios estudiantes registrados.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            filteredStudents.forEach { student ->
                                val isChecked = student.email.trim().lowercase() in state.assignedStudentEmails
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { viewModel.onToggleStudent(student.email) }
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
                }
            }
            item {
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        val effectiveCourse = teacherCourse ?: state.course
                        viewModel.publishWithCourse(
                            effectiveCourse, teacherSubject ?: state.subject,
                            onPublish = onPublish, onPublished = onPublished
                        )
                    },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.loading) CircularProgressIndicator() else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Publicar bitácora")
                    }
                }
            }
            item { EndOfContent("Fin del formulario") }
        }
    }
}
