package app.siatka.family

/**
 * Kontrakt dostępu do pozycji dziecka.
 * ADMIN nie widzi cudzego dziecka. Nie ma tu ukrytego trackera.
 */
enum class ShareMode { off, while_app_open, background }

enum class LinkStatus { pending_code, pending_consent, active, revoked }

data class LocationDecision(val ok: Boolean, val reason: String)

fun mayParentSee(
    viewerId: String,
    childId: String,
    viewerIsAdmin: Boolean,
    linkStatus: LinkStatus?,
    parentUserId: String?,
    consent: ShareMode,
): LocationDecision {
    if (viewerId == childId) return LocationDecision(true, "swoje")
    if (viewerIsAdmin && parentUserId != viewerId) return LocationDecision(false, "admin-nie-widzi")
    if (linkStatus != LinkStatus.active || parentUserId != viewerId) return LocationDecision(false, "brak-parent-link")
    if (consent == ShareMode.off) return LocationDecision(false, "brak-zgody")
    return LocationDecision(true, "parent-link")
}

fun fixFreshness(ageMs: Long): String = if (ageMs <= 180_000L) "aktualna" else "nieaktualna"
