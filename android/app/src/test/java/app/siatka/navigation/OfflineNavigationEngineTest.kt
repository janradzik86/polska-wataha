package app.siatka.navigation

import org.junit.Assert.*
import org.junit.Test

class OfflineNavigationEngineTest {
    private val router=object: OfflineRoutingProvider {
        override fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode)=RouteResult(
            id="r1", mode=mode,
            points=listOf(from,GeoPoint((from.lat+to.lat)/2,(from.lon+to.lon)/2),to),
            steps=listOf(
                RouteStep(Maneuver.START,"Start",from,0.0),
                RouteStep(Maneuver.ARRIVE,"Cel",to,1000.0)
            ),
            distanceM=1000.0,
            packIds=listOf("test")
        )
    }

    @Test fun detectsArrival() {
        val nav=OfflineNavigationEngine(router, NavigationOptions(arrivalDistanceM=20.0))
        val to=GeoPoint(50.01,17.01)
        nav.start(GeoPoint(50.0,17.0),to,TravelMode.WALK)
        assertTrue(nav.update(to).arrived)
    }

    @Test fun suggestsRerouteWhenFarFromPolyline() {
        val nav=OfflineNavigationEngine(router, NavigationOptions(rerouteDistanceM=30.0))
        nav.start(GeoPoint(50.0,17.0),GeoPoint(50.01,17.01),TravelMode.WALK)
        assertTrue(nav.update(GeoPoint(50.02,17.02)).rerouteSuggested)
    }
}
