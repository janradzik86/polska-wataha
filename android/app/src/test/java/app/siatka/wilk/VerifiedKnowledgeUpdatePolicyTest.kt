package app.siatka.wilk

import org.junit.Assert.*
import org.junit.Test

class VerifiedKnowledgeUpdatePolicyTest {
    @Test
    fun crisisModeNeverChecksNetwork() {
        assertFalse(VerifiedKnowledgeUpdatePolicy.canCheck(
            online = true,
            crisisMode = true,
            batteryPercent = 90
        ))
    }

    @Test
    fun verifiedCriticalPackCanInstall() {
        val manifest = VerifiedKnowledgeUpdatePolicy.Manifest(
            id = "first-aid-pl",
            domain = VerifiedKnowledgeUpdatePolicy.Domain.FIRST_AID,
            version = "2026.1",
            publishedAt = "2026-09-22",
            sourceNames = listOf("approved"),
            sourceUrls = listOf("https://example.invalid"),
            sha256 = "abc",
            signature = "sig",
            critical = true
        )
        val result = VerifiedKnowledgeUpdatePolicy.decide(
            currentVersion = null,
            manifest = manifest,
            signatureValid = true,
            hashValid = true
        )
        assertEquals(VerifiedKnowledgeUpdatePolicy.Action.INSTALL, result.action)
    }
}
