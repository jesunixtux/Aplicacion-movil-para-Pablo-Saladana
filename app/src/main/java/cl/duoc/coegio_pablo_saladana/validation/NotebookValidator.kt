package cl.duoc.coegio_pablo_saladana.validation

import cl.duoc.coegio_pablo_saladana.data.NotebookDraft

/** Exige los campos necesarios para publicar una bitácora. */
object NotebookValidator {
    fun isValid(draft: NotebookDraft): Boolean = draft.title.isNotBlank() &&
        draft.purpose.isNotBlank() && draft.instructions.isNotBlank()
}
