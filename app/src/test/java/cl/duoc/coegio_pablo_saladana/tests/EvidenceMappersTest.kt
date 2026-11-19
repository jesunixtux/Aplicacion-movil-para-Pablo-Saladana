package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceDto
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceKind
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceMappers
import cl.duoc.coegio_pablo_saladana.data.remote.dto.EvidenceTransferState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceMappersTest {
    @Test
    fun localFilesArePendingAndNeverShareable() {
        val evidences = EvidenceMappers.fromLocalLines(
            photoUris = "content://foto/1\ncontent://foto/2",
            fileUris = "content://doc/1",
            links = ""
        )
        assertEquals(3, evidences.size)
        assertTrue(evidences.all { it.transferState == EvidenceTransferState.PENDING })
        assertTrue(evidences.all { !it.isShareable })
        assertTrue(evidences.all { it.remoteRef == null && it.url == null })
    }

    @Test
    fun httpLinksAreReadyToShareAndOtherSchemesAreDropped() {
        val evidences = EvidenceMappers.fromLocalLines(
            photoUris = "",
            fileUris = "",
            links = "https://example.org/a\nhttp://example.org/b\njavascript:alert(1)\nftp://x.cl"
        )
        assertEquals(2, evidences.size)
        assertTrue(evidences.all { it.kind == EvidenceKind.LINK })
        assertTrue(evidences.all { it.isShareable })
        assertTrue(evidences.all { it.transferState == EvidenceTransferState.NOT_APPLICABLE })
    }

    @Test
    fun blankLinesAreIgnored() {
        assertTrue(EvidenceMappers.fromLocalLines(" \n ", "\n", "  ").isEmpty())
    }

    @Test
    fun uploadedRemoteReferencesBecomeShareableAgain() {
        val evidences = EvidenceMappers.fromLocalLines(
            photoUris = "https://cdn.example.org/foto.jpg",
            fileUris = "",
            links = ""
        )
        assertEquals(EvidenceTransferState.UPLOADED, evidences[0].transferState)
        assertTrue(evidences[0].isShareable)
        assertEquals("https://cdn.example.org/foto.jpg", evidences[0].remoteRef)
    }

    @Test
    fun onlyUploadedReferencesReturnToLocalLines() {
        val evidences = listOf(
            EvidenceDto(kind = EvidenceKind.PHOTO, transferState = EvidenceTransferState.PENDING),
            EvidenceDto(kind = EvidenceKind.PHOTO, remoteRef = "https://cdn.example.org/ok.jpg", transferState = EvidenceTransferState.UPLOADED),
            EvidenceDto(kind = EvidenceKind.FILE, remoteRef = "https://cdn.example.org/doc.pdf", transferState = EvidenceTransferState.UPLOADED),
            EvidenceDto(kind = EvidenceKind.LINK, url = "https://example.org", transferState = EvidenceTransferState.NOT_APPLICABLE)
        )
        assertEquals("https://cdn.example.org/ok.jpg", EvidenceMappers.toLocalPhotoLines(evidences))
        assertEquals("https://cdn.example.org/doc.pdf", EvidenceMappers.toLocalFileLines(evidences))
        assertEquals("https://example.org", EvidenceMappers.toLocalLinkLines(evidences))
        assertEquals("", EvidenceMappers.toLocalPhotoLines(null))
    }

    @Test
    fun evidenceDtoSurvivesJsonRoundTrip() {
        val gson = Gson()
        val original = EvidenceMappers.fromLocalLines("content://foto/1", "", "https://example.org")
        val type = object : TypeToken<List<EvidenceDto>>() {}.type
        val restored: List<EvidenceDto> = gson.fromJson(gson.toJson(original), type)
        assertEquals(original, restored)
        assertFalse(gson.toJson(original).contains("content://"))
    }
}
