package app.siatka.navigation

/**
 * Adapter BRouter. Nie udaje działającej trasy, dopóki natywny silnik i grafy
 * routingu nie są faktycznie podłączone.
 */
class BrouterRoutingProvider : OfflineRoutingProvider {
    val id: String = "brouter"
    val available: Boolean = false

    override fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): RouteResult {
        error("BRouter is not connected to native routing data")
    }

    fun plan(): RouteResult? = null
}

/**
 * Placeholder pod lokalny graf paczki. Sam fakt posiadania PMTiles/search.db
 * nie oznacza jeszcze, że routing działa.
 */
class PackGraphRoutingProvider : OfflineRoutingProvider {
    val id: String = "pack-graph"
    val available: Boolean = false

    override fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): RouteResult {
        error("Offline routing graph is not connected")
    }

    fun plan(): RouteResult? = null
}
