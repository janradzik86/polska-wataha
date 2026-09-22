package app.siatka.family

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
