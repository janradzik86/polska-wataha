package app.siatka.navigation

interface OfflineSearchProvider {
    fun search(query: String, near: GeoPoint? = null, limit: Int = 20): List<SearchResult>
}

interface OfflineRoutingProvider {
    fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): RouteResult
}

interface OfflineMapPackProvider {
    fun list(): List<OfflineMapPackState>
    fun isCoverageAvailable(point: GeoPoint): Boolean
    fun verify(packId: String): Boolean
}

interface LocationProvider {
    fun current(): GeoPoint?
    fun start(onLocation: (GeoPoint) -> Unit)
    fun stop()
}
