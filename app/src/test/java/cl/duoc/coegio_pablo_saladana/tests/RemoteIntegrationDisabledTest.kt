package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.BuildConfig
import cl.duoc.coegio_pablo_saladana.data.remote.EvidenceUploadResult
import cl.duoc.coegio_pablo_saladana.data.remote.DisabledEvidenceUploader
import cl.duoc.coegio_pablo_saladana.data.remote.NotebookRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.StudentEntryRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceDto
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceKind
import cl.duoc.coegio_pablo_saladana.domain.auth.AuthOutcome
import cl.duoc.coegio_pablo_saladana.domain.auth.RemoteAuthProvider
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La integración remota debe permanecer desactivada y sin credenciales ficticias. */
class RemoteIntegrationDisabledTest {
    @Test
    fun remoteSyncFlagIsOffByDefault() {
        assertFalse(BuildConfig.REMOTE_SYNC_ENABLED)
        assertEquals("http://10.0.2.2:8090/", BuildConfig.API_BASE_URL)
    }

    @Test
    fun notebookDataSourceRefusesToCallBackendWhileDisabled() = runBlocking {
        assertNull(NotebookRemoteDataSource.fetchAll())
        assertEquals(AppErrors.REMOTE_DISABLED, NotebookRemoteDataSource.lastError)
    }

    @Test
    fun entryDataSourceRefusesToCallBackendWhileDisabled() = runBlocking {
        assertNull(StudentEntryRemoteDataSource.fetchByStudent("a@e.cl"))
        assertEquals(AppErrors.REMOTE_DISABLED, StudentEntryRemoteDataSource.lastError)
        assertNull(StudentEntryRemoteDataSource.fetchByTeacher("d@e.cl"))
        assertEquals(AppErrors.REMOTE_DISABLED, StudentEntryRemoteDataSource.lastError)
    }

    @Test
    fun remoteAuthenticationIsPreparedButDisabled() = runBlocking {
        val provider = RemoteAuthProvider()
        assertTrue(provider.isRemote)
        val outcome = provider.login("persona@escuela.cl", "clave-de-prueba")
        assertTrue(outcome is AuthOutcome.Failure)
        assertTrue((outcome as AuthOutcome.Failure).message.contains("no está habilitada"))
    }

    @Test
    fun evidenceUploadIsDisabledAndNeverOpensTheLocalStream() = runBlocking {
        val result = DisabledEvidenceUploader().upload(
            EvidenceDto(kind = EvidenceKind.PHOTO)
        ) { throw AssertionError("El stream local no debe abrirse con la subida desactivada") }
        assertTrue(result is EvidenceUploadResult.Failed)
        assertTrue((result as EvidenceUploadResult.Failed).message.contains("desactivada"))
    }
}
