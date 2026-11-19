package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import org.junit.Assert.assertEquals
import org.junit.Test

class ChileanHolidayTest {
    @Test
    fun convertsIsoDateToFormFormat() {
        assertEquals("18/09/2026", ChileanHoliday("2026-09-18", "Independencia Nacional").formattedDate)
        assertEquals("01/01/2026", ChileanHoliday("2026-01-01", "Año Nuevo").formattedDate)
    }

    @Test
    fun keepsUnexpectedFormatsUntouched() {
        assertEquals("18/09/2026", ChileanHoliday("18/09/2026", "Ya formateado").formattedDate)
        assertEquals("", ChileanHoliday("", "Sin fecha").formattedDate)
        assertEquals("2026-09", ChileanHoliday("2026-09", "Incompleto").formattedDate)
    }
}
