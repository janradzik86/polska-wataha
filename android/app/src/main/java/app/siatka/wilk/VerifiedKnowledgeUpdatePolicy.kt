package app.siatka.wilk

/** Paczka wiedzy tylko z podpisem i https. W kryzysie brak pobierania. */
object VerifiedKnowledgeUpdatePolicy {
    fun mayFetch(crisis: Boolean, online: Boolean): Boolean = !crisis && online

    fun accepts(sourceUrls: List<String>, signed: Boolean): Boolean {
        if (!signed || sourceUrls.isEmpty()) return false
        return sourceUrls.all { it.startsWith("https://") }
    }
}
