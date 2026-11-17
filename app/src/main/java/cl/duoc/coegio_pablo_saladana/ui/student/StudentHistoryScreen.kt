package cl.duoc.coegio_pablo_saladana.ui.student

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
import androidx.compose.material.icons.filled.Edit
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
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.ui.components.AnimatedListItem
import cl.duoc.coegio_pablo_saladana.ui.components.EndOfContent
import cl.duoc.coegio_pablo_saladana.ui.components.EntryEvidence

/** Lista los registros guardados por la cuenta estudiante actual. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHistory(
    entries: List<StudentEntryEntity>,
    onEditDraft: (StudentEntryEntity) -> Unit = {},
    onBack: () -> Unit
) {
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
                    StudentHistoryEntryCard(entry = entry, onEditDraft = { onEditDraft(entry) })
                }
            }
            item { EndOfContent("Llegaste al final del historial") }
        }
    }
}

@Composable
private fun StudentHistoryEntryCard(
    entry: StudentEntryEntity,
    onEditDraft: () -> Unit
) {
    val statusLabel = when {
        entry.isDraft -> "Borrador"
        entry.isReviewed -> "Enviado · Revisado por docente"
        else -> "Enviado · Pendiente de revisión"
    }
    val badgeContainerColor = when {
        entry.isDraft -> MaterialTheme.colorScheme.tertiaryContainer
        entry.isReviewed -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeContentColor = when {
        entry.isDraft -> MaterialTheme.colorScheme.onTertiaryContainer
        entry.isReviewed -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.classDate} · ${entry.notebookTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = badgeContainerColor
                ) {
                    Text(
                        text = statusLabel,
                        color = badgeContentColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = entry.learning.ifBlank { entry.content },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (entry.teacherFeedback.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Retroalimentación docente: ${entry.teacherFeedback}" +
                            (entry.reviewedAt?.let {
                                " · ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "CL")).format(java.util.Date(it))}"
                            } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            EntryEvidence(entry.photoUris, entry.fileUris, entry.links)

            if (entry.isDraft) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedButton(
                    onClick = onEditDraft,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Completar o enviar borrador")
                }
            }
        }
    }
}
