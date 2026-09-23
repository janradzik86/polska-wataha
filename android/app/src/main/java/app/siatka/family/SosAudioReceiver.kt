package app.siatka.family

enum class SosAudioReceiverState { IDLE, CONNECTING, RECEIVING, RECORDING, ENDED, FAILED }

data class SosAudioSessionInfo(
    val id: String,
    val childUserId: String,
    val parentUserId: String,
    val startedAt: String,
    val state: SosAudioReceiverState,
    val encrypted: Boolean = true
)

/**
 * Port pod docelowy WebRTC/MediaRecorder. Sam kontrakt nie udaje działającego transportu.
 */
interface SosAudioReceiverPort {
    fun connect(session: SosAudioSessionInfo)
    fun startLocalRecording()
    fun stopLocalRecording(): String?
    fun disconnect()
}

/**
 * Bezpieczny stan odbiornika po stronie opiekuna.
 * Nagrywanie zaczyna się dopiero po pojawieniu się prawdziwej zdalnej ścieżki audio.
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
        return "SOS_${safe}_${stamp}.m4a"
    }
}

object SosAudioReceiverAccess {
    fun canReceive(
        activeParentLink: Boolean,
        authenticatedParentId: String,
        session: SosAudioSessionInfo
    ): Boolean = activeParentLink && authenticatedParentId == session.parentUserId
}
