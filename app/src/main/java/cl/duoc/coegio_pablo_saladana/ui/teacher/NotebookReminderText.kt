package cl.duoc.coegio_pablo_saladana.ui.teacher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import java.util.Calendar

/** Recordatorios internos al entrar al panel: no envía correos ni notificaciones. */
@Composable
fun NotebookReminderText(notebook: NotebookEntity) {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH) + 1
    val periodHasEnded = notebook.year < currentYear ||
        (notebook.year == currentYear && notebook.month < currentMonth)
    val lastDay = now.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysLeft = lastDay - now.get(Calendar.DAY_OF_MONTH)
    val messages = mutableListOf<String>()
    if (periodHasEnded) {
        messages += "Recordatorio: el periodo ya terminó; cierra esta bitácora."
        messages += "Prepara la bitácora del siguiente mes."
    } else if (notebook.year == currentYear && notebook.month == currentMonth && daysLeft <= 7) {
        messages += "Quedan $daysLeft días para el cierre mensual. Prepara el siguiente mes."
    }
    if (currentMonth == 2 && daysLeft <= 14 || currentMonth == 6 && daysLeft <= 14) {
        messages += "Recordatorio: se aproxima el inicio del siguiente semestre."
    }
    messages.forEach { message ->
        Text(message, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
    }
}
