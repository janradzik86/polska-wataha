package app.siatka.navigation

class MemoryOfflineSearchProvider(
    private val items: List<SearchResult>
): OfflineSearchProvider {
    override fun search(query: String, near: GeoPoint?, limit: Int): List<SearchResult> {
        val q=query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return items
            .filter { (it.name+" "+(it.subtitle ?: "")+" "+(it.category ?: "")).lowercase().contains(q) }
            .sortedWith(compareBy {
                if (near == null) 0.0 else GeoMath.haversineMeters(near,it.point)
            })
            .take(limit)
    }
}
