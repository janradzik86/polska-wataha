package app.siatka.navigation

/**
 * Adapter BRouter. available = false, dopóki nie ma natywnej binarki i paczek odcinków.
 * plan nie zwraca wymyślonej trasy.
 */
interface OfflineRoutingProvider {
    val id: String
    val available: Boolean
}

class BrouterRoutingProvider : OfflineRoutingProvider {
    override val id: String = "brouter"
    override val available: Boolean = false

    fun plan(): String? = null
}

class PackGraphRoutingProvider : OfflineRoutingProvider {
    override val id: String = "pack-graph"
    override val available: Boolean = true
}
