package cl.duoc.coegio_pablo_saladana.domain.model

/** Feriado oficial de Chile usado para contextualizar el calendario escolar. */
data class ChileanHoliday(
    val date: String,
    val localName: String
) {
    /** Convierte yyyy-MM-dd al formato dd/MM/yyyy que usa el formulario de registro. */
    val formattedDate: String
        get() {
            val parts = date.split('-')
            return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else date
        }
}
