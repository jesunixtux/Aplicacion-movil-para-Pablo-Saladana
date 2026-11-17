package cl.duoc.coegio_pablo_saladana.ui.teacher

import android.content.ContentResolver
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Informe del MVP generado directamente en Android, sin servicios externos. */
object MonthlyReportWriter {
    fun write(
        resolver: ContentResolver,
        uri: Uri,
        notebook: NotebookEntity,
        students: List<UserAccount>,
        entries: List<StudentEntryEntity>
    ): Boolean = runCatching {
        val pdf = PdfDocument()
        try {
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL"))
            val participants = entries.map { it.studentEmail.trim().lowercase() }.toSet()
            val absent = students.filter { it.email.trim().lowercase() !in participants }
            val lines = mutableListOf(
                "BITÁCORA MENSUAL DE APRENDIZAJE",
                "Curso: ${notebook.course} | Asignatura: ${notebook.subject}",
                "Periodo: ${notebook.periodLabel} | Docente: ${notebook.teacherName}",
                "Título: ${notebook.title}",
                "Propósito: ${notebook.purpose}",
                "Publicado: ${dateFormatter.format(Date(notebook.publishedAt))}",
                "Cerrado: ${notebook.closedAt?.let { dateFormatter.format(Date(it)) } ?: "Pendiente"}",
                "Informe generado: ${dateFormatter.format(Date())}",
                "",
                "RESUMEN DE PARTICIPACIÓN",
                "Estudiantes totales: ${students.size}",
                "Estudiantes que participaron: ${students.size - absent.size}",
                "Estudiantes sin participación: ${absent.size}",
                "Total de aportes: ${entries.size}",
                "Aportes revisados: ${entries.count { it.isReviewed }}",
                "Aportes pendientes: ${entries.count { !it.isReviewed }}",
                "Sin participación: ${absent.joinToString { it.fullName }.ifBlank { "Ninguno" }}",
                "",
                "TEMAS Y ACTIVIDADES POR FECHA"
            )
            entries.sortedWith(compareBy<StudentEntryEntity> { it.classDate.split('/').reversed().joinToString("") }
                .thenBy { it.createdAt }).forEach { entry ->
                lines += "${entry.classDate} - ${entry.studentName} - ${if (entry.isReviewed) "Revisado" else "Pendiente"}"
                lines += "Tema: ${entry.content}"
                lines += "Actividad: ${entry.activity}"
            }
            if (entries.isEmpty()) lines += "No se registraron aportes este mes."

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var y = 50f
            fun newPage() {
                page?.let(pdf::finishPage)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                y = 50f
            }
            newPage()
            for (raw in lines) {
                if (raw.isEmpty()) {
                    y += 10f
                    continue
                }
                var remaining = raw.replace('\n', ' ')
                while (remaining.isNotEmpty()) {
                    if (y > 798f) newPage()
                    val width = paint.breakText(remaining, true, 520f, null).coerceAtLeast(1)
                    val part = remaining.take(width)
                    page!!.canvas.drawText(part, 38f, y, paint)
                    y += 17f
                    remaining = remaining.drop(width).trimStart()
                }
            }
            page?.let(pdf::finishPage)
            requireNotNull(resolver.openOutputStream(uri)).use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        true
    }.getOrDefault(false)
}
