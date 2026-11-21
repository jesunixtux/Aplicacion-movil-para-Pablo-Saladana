package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La compilación normal usa el backend local real, sin credenciales incrustadas. */
class RemoteIntegrationDisabledTest {
    @Test
    fun remoteSyncIsEnabledForTheLocalBackend() {
        assertTrue(BuildConfig.REMOTE_SYNC_ENABLED)
        assertEquals("http://10.0.2.2:8090/", BuildConfig.API_BASE_URL)
    }
}
