package app.siatka.family

/**
 * Odbiornik SOS po stronie opiekuna.
 * Nie nagrywa, dopóki nie ma prawdziwej ścieżki audio z WebRTC.
 * Plik ma powstać na telefonie rodzica, nie na serwerze.
 */
class SosAudioReceiver {
    var recording: Boolean = false
        private set

    fun authorize(viewerId: String, childId: String, parentId: String, linkActive: Boolean): Boolean {
        if (!linkActive) return false
        return viewerId == childId || viewerId == parentId
    }

    fun onRemoteAudioTrack(present: Boolean): String {
        if (!present) {
            recording = false
            return "Łączenie z mikrofonem dziecka…"
        }
        recording = true
        return "Odbieram dźwięk. Nagranie zostaje na tym urządzeniu."
    }

    fun stop() {
        recording = false
    }

    fun fileName(childName: String, stamp: String): String {
        val safe = childName.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifEmpty { "dziecko" }
        return "SOS_${safe}_$stamp.m4a"
    }
}
