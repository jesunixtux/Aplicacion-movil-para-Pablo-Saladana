package cl.duoc.coegio_pablo_saladana.domain.model

/** Información editable de un registro antes de guardarlo como borrador o enviarlo. */
data class StudentEntryDraft(
    val classDate: String,
    val content: String,
    val activity: String,
    val learning: String,
    val doubts: String,
    val photoUris: String = "",
    val fileUris: String = "",
    val links: String = ""
) {
    fun normalized(): StudentEntryDraft = copy(
        classDate = classDate.trim(),
        content = content.trim(),
        activity = activity.trim(),
        learning = learning.trim(),
        doubts = doubts.trim(),
        photoUris = photoUris.trim(),
        fileUris = fileUris.trim(),
        links = links.trim()
    )
}
