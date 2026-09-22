package app.siatka.navigation

import kotlin.math.*

object GeoMath {
    private const val EARTH_M = 6371000.0

    fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val la1 = Math.toRadians(a.lat)
        val la2 = Math.toRadians(b.lat)
        val h = sin(dLat/2).pow(2) + cos(la1) * cos(la2) * sin(dLon/2).pow(2)
        return 2 * EARTH_M * asin(min(1.0, sqrt(h)))
    }

    fun polylineLengthMeters(points: List<GeoPoint>): Double =
        points.zipWithNext().sumOf { (a,b) -> haversineMeters(a,b) }

    fun nearestPoint(points: List<GeoPoint>, current: GeoPoint): Pair<Int,Double> {
        if (points.isEmpty()) return -1 to Double.POSITIVE_INFINITY
        var bestIndex=0
        var bestDistance=Double.POSITIVE_INFINITY
        points.forEachIndexed { index, p ->
            val d=haversineMeters(p,current)
            if (d<bestDistance) { bestIndex=index; bestDistance=d }
        }
        return bestIndex to bestDistance
    }

    fun remainingDistanceMeters(points: List<GeoPoint>, fromIndex: Int, current: GeoPoint? = null): Double {
        if (points.isEmpty() || fromIndex !in points.indices) return 0.0
        var total = current?.let { haversineMeters(it,points[fromIndex]) } ?: 0.0
        for (i in (fromIndex+1) until points.size) total += haversineMeters(points[i-1],points[i])
        return total
    }
}
