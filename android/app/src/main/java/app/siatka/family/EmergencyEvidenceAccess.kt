package app.siatka.family

data class EmergencyEvidenceItem(
    val id: String,
    val childUserId: String,
    val startedAt: String,
    val endedAt: String?,
    val lat: Double?,
    val lon: Double?,
    val accuracyM: Double?,
    val sha256: String?,
    val available: Boolean
)

data class EmergencyEvidencePermission(
    val activeParentLink: Boolean,
    val childUserId: String,
    val parentUserId: String
)

object EmergencyEvidenceAccess {
    fun canView(permission: EmergencyEvidencePermission): Boolean =
        permission.activeParentLink

    fun requireView(permission: EmergencyEvidencePermission) {
        check(canView(permission)) { "403: brak dostępu do materiału SOS dziecka" }
    }
}
