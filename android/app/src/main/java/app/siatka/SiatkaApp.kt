package app.siatka

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SiatkaApp : Application() {
    lateinit var db: SiatkaDb
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = SiatkaDb(this)
        db.seedIfEmpty()
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel("messages", "Wiadomości", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("exchanges", "Wymiany", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("help", "Pomoc", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("local", "Lokalne", NotificationManager.IMPORTANCE_LOW),
            NotificationChannel("crisis", "Kryzysowe", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerty kryzysowe — najwyższy priorytet zgodny z ustawieniami"
                enableVibration(true)
            },
        )
        channels.forEach { nm.createNotificationChannel(it) }
    }

    companion object {
        lateinit var instance: SiatkaApp
            private set
    }
}
