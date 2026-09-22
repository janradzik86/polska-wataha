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

interface SosAudioReceiver {
    fun connect(session: SosAudioSessionInfo)
    fun startLocalRecording()
    fun stopLocalRecording(): String?
    fun disconnect()
}

/**
 * Uprawnienie do odbioru audio SOS wynika wyłącznie z aktywnego Family Bridge.
 * Serwer musi dodatkowo sprawdzić, że parentUserId z sesji jest bieżącym użytkownikiem.
 */
object SosAudioReceiverAccess {
    fun canReceive(activeParentLink: Boolean, authenticatedParentId: String, session: SosAudioSessionInfo): Boolean =
        activeParentLink && authenticatedParentId == session.parentUserId
}
