package cl.duoc.coegio_pablo_saladana.validation

import cl.duoc.coegio_pablo_saladana.data.NotebookDraft

/** Exige los campos necesarios para publicar una bitácora. */
object NotebookValidator {
    data class Errors(val title: Boolean, val purpose: Boolean, val instructions: Boolean) {
        val isValid: Boolean get() = !title && !purpose && !instructions
    }

    fun validate(draft: NotebookDraft): Errors = Errors(
        title = draft.title.isBlank(),
        purpose = draft.purpose.isBlank(),
        instructions = draft.instructions.isBlank()
    )

    fun isValid(draft: NotebookDraft): Boolean = validate(draft).isValid
}
