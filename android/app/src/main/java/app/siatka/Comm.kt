package app.siatka

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

data class AdapterStatus(
    val id: String, val label: String, val layer: String, val health: String, val detail: String, val stage: String,
)

object Comm {
    fun online(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun adapters(ctx: Context, simulateOffline: Boolean): List<AdapterStatus> {
        val net = online(ctx) && !simulateOffline
        val bt = try {
            (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        } catch (_: Exception) { null }
        val btOn = try { bt?.isEnabled == true } catch (_: SecurityException) { false }
        val (loraHealth, loraDetail) = LoraRadio.health()
        return listOf(
            AdapterStatus(
                "internet", "InternetAdapter", "IP / HTTPS",
                if (net) "up" else "down",
                if (net) "Kanał IP aktywny." else "Brak IP. Ruch w kolejce store-and-forward.",
                "V0.4 — działający",
            ),
            AdapterStatus(
                "bluetooth", "BluetoothAdapter", "BLE 5",
                if (btOn) "up" else "ready",
                if (btOn) "Radio BT włączone — skan bramki Heltec V4."
                else "Włącz Bluetooth, żeby sparować moduł LoRa.",
                "V0.4 — łącze do Heltec V4",
            ),
            AdapterStatus(
                "wifi_direct", "WifiDirectAdapter", "Wi-Fi P2P", "ready",
                "Warstwa P2P gotowa. LoRa idzie przez BLE, nie przez Wi-Fi Direct.",
                "V0.3 — przygotowany",
            ),
            AdapterStatus(
                "lora", "LoRaAdapter", "SX1262 · EU868",
                loraHealth, loraDetail,
                "V0.4 — Heltec WiFi LoRa 32 V4",
            ),
        )
    }

    data class Hop(val via: List<String>, val ok: Boolean, val reason: String?)

    fun route(nodes: List<Node>, from: String, to: String): Hop {
        val order = listOf("NODE_A", "NODE_B", "NODE_C", "NODE_D")
        val down = nodes.filter { it.status == "down" }.map { it.id }.toSet()
        val i = order.indexOf(from)
        val j = order.indexOf(to)
        if (i < 0 || j < 0) return Hop(emptyList(), false, "Nieznany węzeł")
        val dir = if (i < j) 1 else -1
        val via = mutableListOf<String>()
        var k = i
        while (k != j) {
            val hop = order[k]
            if (down.contains(hop) && hop != from) {
                val alt = order.filter { !down.contains(it) }
                return if (alt.contains(from) && alt.contains(to) && alt.size >= 2)
                    Hop(alt.filter { it != from && it != to }, true, "$hop niedostępny — rerouting")
                else Hop(via, false, "Przerwany łańcuch przy $hop")
            }
            if (hop != from) via += hop
            k += dir
        }
        return Hop(via, true, null)
    }
}
