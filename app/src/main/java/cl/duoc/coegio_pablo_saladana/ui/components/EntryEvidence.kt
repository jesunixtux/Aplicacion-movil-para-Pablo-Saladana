package cl.duoc.coegio_pablo_saladana.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Muestra únicamente las evidencias de un registro al que la pantalla ya autorizó el acceso. */
@Composable
fun EntryEvidence(photoUris: String, fileUris: String, links: String) {
    val context = LocalContext.current
    val photos = photoUris.lineSequence().filter(String::isNotBlank).toList()
    val files = fileUris.lineSequence().filter(String::isNotBlank).toList()
    val urls = links.lineSequence().filter(String::isNotBlank).toList()
    if (photos.isEmpty() && files.isEmpty() && urls.isEmpty()) return
    Column {
        Text("Evidencias", style = MaterialTheme.typography.titleSmall)
        (photos.mapIndexed { i, uri -> "Fotografía ${i + 1}" to uri } +
            files.mapIndexed { i, uri -> "Archivo ${i + 1}" to uri } +
            urls.mapIndexed { i, url -> "Enlace ${i + 1}: $url" to url }).forEach { (label, path) ->
            TextButton(onClick = {
                val uri = runCatching { Uri.parse(path) }.getOrNull() ?: return@TextButton
                if (uri.scheme !in listOf("content", "http", "https")) return@TextButton
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(intent) }
            }) {
                Text(label)
            }
        }
    }
}
