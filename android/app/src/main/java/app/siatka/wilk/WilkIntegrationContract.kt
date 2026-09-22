package app.siatka.wilk

/**
 * Kontrakt integracyjny pomiędzy WILKIEM a docelową aplikacją Polska Wataha.
 * WILK sugeruje akcję; aplikacja decyduje, czy dana funkcja jest faktycznie dostępna.
 */
enum class WilkAction {
    OPEN_CRISIS_MODE,
    CALL_112,
    OPEN_OFFLINE_MAP,
    SHOW_MY_LOCATION,
    OPEN_LORA,
    OPEN_MESH,
    OPEN_SURVIVAL_GUIDE,
    CHECK_KNOWLEDGE_UPDATES,
    OPEN_NAVIGATION
}

data class WilkActionSuggestion(
    val action: WilkAction,
    val label: String,
    val requiresInternet: Boolean = false
)

object WilkIntegrationContract {
    fun actionsFor(topic: String?, crisis: Boolean): List<WilkActionSuggestion> {
        if (crisis) {
            return listOf(
                WilkActionSuggestion(WilkAction.OPEN_CRISIS_MODE, "🚨 Tryb kryzysowy"),
                WilkActionSuggestion(WilkAction.CALL_112, "📞 112")
            )
        }

        return when {
            topic == "Mapa" || topic == "Gdzie jestem" || topic == "Nawigacja" -> listOf(
                WilkActionSuggestion(WilkAction.OPEN_OFFLINE_MAP, "🗺️ Mapa offline"),
                WilkActionSuggestion(WilkAction.SHOW_MY_LOCATION, "📍 Moja pozycja"),
                WilkActionSuggestion(WilkAction.OPEN_NAVIGATION, "🧭 Nawiguj offline")
            )
            topic?.startsWith("LoRa") == true -> listOf(
                WilkActionSuggestion(WilkAction.OPEN_LORA, "📡 LoRa")
            )
            topic == "Mesh i LoRa" || topic == "Offline" -> listOf(
                WilkActionSuggestion(WilkAction.OPEN_MESH, "📡 Łączność offline")
            )
            topic in setOf("Woda", "Ogień", "Pierwsza pomoc", "Schronienie", "Checklista 72h") -> listOf(
                WilkActionSuggestion(WilkAction.OPEN_SURVIVAL_GUIDE, "🏕️ Poradnik")
            )
            topic == "Aktualizacje wiedzy" -> listOf(
                WilkActionSuggestion(
                    WilkAction.CHECK_KNOWLEDGE_UPDATES,
                    "🔄 Sprawdź aktualizacje wiedzy",
                    requiresInternet = true
                )
            )
            else -> emptyList()
        }
    }
}
