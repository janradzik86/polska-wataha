package app.siatka.navigation

enum class TravelMode { WALK, BIKE, CAR }
enum class Maneuver { START, CONTINUE, TURN_LEFT, TURN_RIGHT, U_TURN, ARRIVE }

data class GeoPoint(val lat: Double, val lon: Double)

data class SearchResult(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val point: GeoPoint,
    val category: String? = null,
    val packId: String
)

data class RouteStep(
    val maneuver: Maneuver,
    val instruction: String,
    val point: GeoPoint,
    val distanceFromStartM: Double
)

data class RouteResult(
    val id: String,
    val mode: TravelMode,
    val points: List<GeoPoint>,
    val steps: List<RouteStep>,
    val distanceM: Double,
    val durationSec: Long? = null,
    val generatedOffline: Boolean = true,
    val packIds: List<String> = emptyList()
)

data class NavigationProgress(
    val active: Boolean,
    val routeId: String? = null,
    val distanceToRouteM: Double? = null,
    val distanceToDestinationM: Double? = null,
    val nextStep: RouteStep? = null,
    val nextStepDistanceM: Double? = null,
    val rerouteSuggested: Boolean = false,
    val arrived: Boolean = false
)

data class OfflineMapPackManifest(
    val id: String,
    val name: String,
    val regionType: String,
    val version: String,
    val bbox: List<Double>,
    val mapFile: String,
    val searchFile: String,
    val routingPath: String,
    val sha256: String? = null,
    val signature: String? = null
)

data class OfflineMapPackState(
    val manifest: OfflineMapPackManifest,
    val installed: Boolean,
    val verified: Boolean,
    val installedAt: String? = null
)
