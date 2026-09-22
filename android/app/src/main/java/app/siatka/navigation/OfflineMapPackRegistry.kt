package app.siatka.navigation

object MapPackGeometry {
    fun pointInsideBbox(point: GeoPoint, bbox: List<Double>): Boolean {
        require(bbox.size == 4) { "bbox must be [minLon,minLat,maxLon,maxLat]" }
        val (minLon,minLat,maxLon,maxLat)=bbox
        return point.lon in minLon..maxLon && point.lat in minLat..maxLat
    }
}

class OfflineMapPackRegistry(initial: List<OfflineMapPackState> = emptyList()) {
    private val states = initial.toMutableList()

    fun list(): List<OfflineMapPackState> = states.toList()

    fun covering(point: GeoPoint): List<OfflineMapPackState> =
        states.filter { it.installed && it.verified && MapPackGeometry.pointInsideBbox(point,it.manifest.bbox) }

    fun canNavigate(from: GeoPoint, to: GeoPoint): Boolean =
        covering(from).isNotEmpty() && covering(to).isNotEmpty()

    fun update(state: OfflineMapPackState) {
        val i=states.indexOfFirst { it.manifest.id==state.manifest.id }
        if (i>=0) states[i]=state else states += state
    }
}
