package cl.duoc.coegio_pablo_saladana.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import cl.duoc.coegio_pablo_saladana.data.remote.EvidenceRemoteDataSource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Muestra únicamente las evidencias de un registro al que la pantalla ya autorizó el acceso. */
@Composable
fun EntryEvidence(photoUris: String, fileUris: String, links: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
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
                val remoteId = path.toLongOrNull()
                if (remoteId != null) {
                    scope.launch {
                        val downloaded = EvidenceRemoteDataSource.download(remoteId)
                        if (downloaded == null) {
                            error = EvidenceRemoteDataSource.lastError
                            return@launch
                        }
                        val uri = withContext(Dispatchers.IO) {
                            val directory = File(context.cacheDir, "downloaded-evidences").apply { mkdirs() }
                            val file = File(directory, downloaded.fileName).apply { writeBytes(downloaded.bytes) }
                            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        }
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setDataAndType(uri, downloaded.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { context.startActivity(intent) }
                            .onFailure { error = "No hay una aplicación para abrir esta evidencia." }
                    }
                    return@TextButton
                }
                val uri = runCatching { Uri.parse(path) }.getOrNull() ?: return@TextButton
                if (uri.scheme !in listOf("content", "http", "https")) return@TextButton
                val intent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                runCatching { context.startActivity(intent) }
            }) {
                Text(label)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
