package app.siatka.wilk

/**
 * Polityka aktualizacji wiedzy WILKA.
 *
 * WILK pozostaje local-first. Internet służy tylko do pobrania zweryfikowanych
 * pakietów wiedzy. Tryb kryzysowy zawsze korzysta z ostatniej poprawnie
 * zweryfikowanej lokalnej wersji.
 */
object VerifiedKnowledgeUpdatePolicy {
    /** Compatibility helper used by the app shell. */
    fun mayFetch(crisis: Boolean, online: Boolean): Boolean = !crisis && online

    /** Minimal source gate before cryptographic verification. */
    fun accepts(sourceUrls: List<String>, signed: Boolean): Boolean {
        if (!signed || sourceUrls.isEmpty()) return false
        return sourceUrls.all { it.startsWith("https://") }
    }

    enum class Domain {
        LAW,
        FIRST_AID,
        CRISIS,
        CIVIC_RIGHTS,
        SAFETY
    }

    data class Manifest(
        val id: String,
        val domain: Domain,
        val version: String,
        val publishedAt: String,
        val sourceNames: List<String>,
        val sourceUrls: List<String>,
        val sha256: String,
        val signature: String,
        val critical: Boolean
    )

    enum class Action { IGNORE, OFFER, INSTALL }

    data class Decision(val action: Action, val reason: String)

    fun canCheck(
        online: Boolean,
        crisisMode: Boolean,
        batteryPercent: Int? = null
    ): Boolean {
        if (!online) return false
        if (crisisMode) return false
        if (batteryPercent != null && batteryPercent < 15) return false
        return true
    }

    fun decide(
        currentVersion: String?,
        manifest: Manifest,
        signatureValid: Boolean,
        hashValid: Boolean
    ): Decision {
        if (!signatureValid || !hashValid) {
            return Decision(Action.IGNORE, "Pakiet nie przeszedł weryfikacji integralności lub podpisu.")
        }
        if (currentVersion == manifest.version) {
            return Decision(Action.IGNORE, "Ta wersja jest już zainstalowana.")
        }
        return if (manifest.critical) {
            Decision(Action.INSTALL, "Zweryfikowana aktualizacja wiedzy krytycznej.")
        } else {
            Decision(Action.OFFER, "Dostępna jest nowsza zweryfikowana wersja wiedzy.")
        }
    }

    val rules = listOf(
        "Brak internetu nie blokuje WILKA.",
        "WILK nie uczy się automatycznie z przypadkowych stron WWW.",
        "Pakiet musi mieć wersję, źródła, SHA-256 i podpis.",
        "Prawo i pierwsza pomoc wymagają zatwierdzonych źródeł.",
        "Nowa wersja nie usuwa ostatniej działającej wersji przed pełną walidacją.",
        "W Trybie Kryzysowym nie pobieramy aktualizacji."
    )
}
