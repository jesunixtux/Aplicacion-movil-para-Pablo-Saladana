package cl.duoc.coegio_pablo_saladana.domain.validation

import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft

/** Exige los campos necesarios para publicar una bitácora. */
object NotebookValidator {
    data class Errors(
        val title: Boolean = false,
        val purpose: Boolean = false,
        val instructions: Boolean = false
    ) {
        val isValid: Boolean get() = !title && !purpose && !instructions
    }

    fun validate(draft: NotebookDraft): Errors = Errors(
        title = draft.title.isBlank(),
        purpose = draft.purpose.isBlank(),
        instructions = draft.instructions.isBlank()
    )

    fun isValid(draft: NotebookDraft): Boolean = validate(draft).isValid
}
