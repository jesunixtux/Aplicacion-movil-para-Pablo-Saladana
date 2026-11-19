package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.ui.components.AppMotionDefaults
import cl.duoc.coegio_pablo_saladana.ui.components.greetingFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GreetingAndMotionTest {
    @Test
    fun greetingUsesTheChosenGender() {
        assertEquals("Hola Sr. Juan Pérez", greetingFor("Juan Pérez", UserGender.Male))
        assertEquals("Hola Sra. Ana Pérez", greetingFor("Ana Pérez", UserGender.Female))
        assertEquals("Hola, Ana Pérez", greetingFor("Ana Pérez", UserGender.Other))
    }

    @Test
    fun greetingKeepsTheFullNameEvenWhenEmpty() {
        assertEquals("Hola, ", greetingFor("", UserGender.Other))
    }

    @Test
    fun motionDurationsArePositiveAndOrdered() {
        assertTrue(AppMotionDefaults.NavigationDurationMillis > 0)
        assertTrue(AppMotionDefaults.ItemDurationMillis > 0)
        assertTrue(AppMotionDefaults.ItemDelayMillis > 0)
        assertTrue(AppMotionDefaults.EndOfContentPulseMillis > 0)
        assertTrue(AppMotionDefaults.ItemDelayMillis < AppMotionDefaults.ItemDurationMillis)
    }

    @Test
    fun staggerDelayIsCappedForLongLists() {
        val delayForTenthItem = (10 * AppMotionDefaults.ItemDelayMillis)
            .coerceAtMost(AppMotionDefaults.MaximumStaggerDelayMillis)
        assertEquals(AppMotionDefaults.MaximumStaggerDelayMillis, delayForTenthItem)
        assertEquals(
            AppMotionDefaults.MaximumStaggerDelayMillis,
            5 * AppMotionDefaults.ItemDelayMillis
        )
    }

    @Test
    fun animationScalesStayBelowFullSize() {
        assertTrue(AppMotionDefaults.ItemInitialScale in 0f..1f)
        assertTrue(AppMotionDefaults.EndOfContentMinimumScale in 0f..1f)
        assertTrue(AppMotionDefaults.EndOfContentMinimumScale < AppMotionDefaults.ItemInitialScale)
    }
}
