package app.siatka.navigation

data class NavigationOptions(
    val rerouteDistanceM: Double = 45.0,
    val arrivalDistanceM: Double = 25.0,
    val stepAdvanceDistanceM: Double = 30.0
)

class OfflineNavigationEngine(
    private val router: OfflineRoutingProvider,
    private val options: NavigationOptions = NavigationOptions()
) {
    private var route: RouteResult? = null
    private var destination: GeoPoint? = null
    private var mode: TravelMode = TravelMode.WALK
    private var stepIndex = 0

    fun start(from: GeoPoint, to: GeoPoint, mode: TravelMode): RouteResult {
        val next = router.route(from,to,mode)
        require(next.points.isNotEmpty()) { "Router returned empty route" }
        route=next
        destination=to
        this.mode=mode
        stepIndex=0
        return next
    }

    fun stop() {
        route=null
        destination=null
        stepIndex=0
    }

    fun currentRoute(): RouteResult? = route

    fun update(current: GeoPoint): NavigationProgress {
        val r=route ?: return NavigationProgress(active=false)
        val dest=destination ?: return NavigationProgress(active=false)

        val (nearestIndex,distanceToRoute)=GeoMath.nearestPoint(r.points,current)
        val directToDestination=GeoMath.haversineMeters(current,dest)
        val arrived=directToDestination<=options.arrivalDistanceM

        while (stepIndex < r.steps.lastIndex) {
            val d=GeoMath.haversineMeters(current,r.steps[stepIndex].point)
            if (d<=options.stepAdvanceDistanceM) stepIndex++ else break
        }

        val step=r.steps.getOrNull(stepIndex)
        return NavigationProgress(
            active=!arrived,
            routeId=r.id,
            distanceToRouteM=distanceToRoute,
            distanceToDestinationM=GeoMath.remainingDistanceMeters(r.points,maxOf(0,nearestIndex),current),
            nextStep=step,
            nextStepDistanceM=step?.let { GeoMath.haversineMeters(current,it.point) },
            rerouteSuggested=!arrived && distanceToRoute>options.rerouteDistanceM,
            arrived=arrived
        )
    }

    fun reroute(current: GeoPoint): RouteResult {
        val dest=destination ?: error("Navigation not active")
        return start(current,dest,mode)
    }
}
