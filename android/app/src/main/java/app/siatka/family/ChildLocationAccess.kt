package app.siatka.family

/**
 * Dostęp do pozycji dziecka tylko przez aktywny Family Bridge i zgodę.
 * ADMIN bez relacji rodzic-dziecko nie dostaje lokalizacji.
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

fun fixFreshness(ageMs: Long): String =
    if (ageMs <= 180_000L) "aktualna" else "nieaktualna"

data class ChildLocationView(
    val childUserId: String,
    val lat: Double,
    val lon: Double,
    val accuracyM: Double? = null,
    val recordedAt: String,
    val stale: Boolean
)

data class ChildLocationPermission(
    val childUserId: String,
    val activeParentLink: Boolean,
    val sharingEnabled: Boolean
)

object ChildLocationAccess {
    fun canView(permission: ChildLocationPermission): Boolean =
        permission.activeParentLink && permission.sharingEnabled

    fun requireView(permission: ChildLocationPermission) {
        check(canView(permission)) { "403: brak uprawnienia do lokalizacji dziecka" }
    }
}
