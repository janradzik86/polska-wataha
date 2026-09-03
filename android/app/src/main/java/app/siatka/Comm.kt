package app.siatka

import android.bluetooth.BluetoothAdapter
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
        val bt = try { BluetoothAdapter.getDefaultAdapter() } catch (_: SecurityException) { null }
        val btState = when {
            bt == null -> "ready"
            !bt.isEnabled -> "ready"
            else -> "ready"
        }
        return listOf(
            AdapterStatus(
                "internet", "InternetAdapter", "IP / HTTPS",
                if (net) "up" else "down",
                if (net) "Kanał IP aktywny." else "Brak IP. Ruch w kolejce store-and-forward.",
                "V0.1 — działający",
            ),
            AdapterStatus(
                "bluetooth", "BluetoothAdapter", "BLE / Classic", btState,
                if (bt == null) "Adapter przygotowany. Brak uprawnienia lub radia — nie udajemy połączenia."
                else "Radio BT wykryte. Skan i ramki testowe gotowe (V0.3).",
                "V0.3 — przygotowany do testów",
            ),
            AdapterStatus(
                "wifi_direct", "WifiDirectAdapter", "Wi-Fi P2P", "ready",
                "Negocjacja grupy P2P zaimplementowana jako warstwa. Czeka na testy urządzenie–urządzenie.",
                "V0.3 — przygotowany do testów",
            ),
            AdapterStatus(
                "lora", "LoRaAdapter", "LoRa / SX126x", "hardware_missing",
                "Brak modułu LoRa w tym telefonie. Adapter nie udaje transmisji radiowej.",
                "V0.4 — warstwa pod przyszły sprzęt",
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
