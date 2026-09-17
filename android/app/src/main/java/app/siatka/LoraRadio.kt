package app.siatka

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.UUID

data class LoraFound(val address: String, val name: String, val rssi: Int)

@SuppressLint("MissingPermission")
object LoraRadio {
    val NUS_SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val NUS_RX: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val NUS_TX: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    enum class Phase { Idle, Scanning, Connecting, Linked, Error }

    @Volatile var phase = Phase.Idle
    @Volatile var detail = "Heltec WiFi LoRa 32 V4 niepodłączony. Pasmo EU868."
    @Volatile var deviceName: String? = null
    @Volatile var deviceAddress: String? = null
    @Volatile var lastRssi: Int? = null
    @Volatile var firmware: String? = null
    @Volatile var band = "868.1 MHz"
    var pairKey: String? = null

    val found = mutableListOf<LoraFound>()
    val log = mutableListOf<String>()

    private val listeners = mutableListOf<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var rxChar: BluetoothGattCharacteristic? = null
    private var adapter: BluetoothAdapter? = null
    private var scanning = false
    private val rxBuf = StringBuilder()
    private var pendingPin: String? = null
    private var pairTimeout: Runnable? = null

    fun onChange(cb: (() -> Unit)?) {
        if (cb == null) return
        if (!listeners.contains(cb)) listeners += cb
    }
    fun removeChange(cb: () -> Unit) { listeners.remove(cb) }
    private fun bump() { main.post { listeners.toList().forEach { it() } } }

    fun health(): Pair<String, String> = when (phase) {
        Phase.Linked -> "up" to "Radio SX1262 po BLE · $band · ${deviceName ?: "Heltec V4"}"
        Phase.Scanning -> "ready" to "Skanowanie BLE — szukam WATAHA-*"
        Phase.Connecting -> "ready" to "Łączenie z ${deviceName ?: "modułem"}… czekam na PIN."
        Phase.Error -> "down" to detail
        Phase.Idle -> "ready" to "Adapter LoRa gotowy. Podłącz Heltec WiFi LoRa 32 V4 (HF 863–928)."
    }

    fun startScan(ctx: Context) {
        val mgr = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = mgr.adapter
        val bt = adapter
        if (bt == null || !bt.isEnabled) {
            phase = Phase.Error
            detail = "Bluetooth wyłączony. Włącz radio BT w telefonie."
            bump(); return
        }
        found.clear()
        phase = Phase.Scanning
        detail = "Skanowanie BLE (WATAHA- / Heltec)…"
        scanning = true
        bump()
        val scanner = bt.bluetoothLeScanner
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(null, settings, scanCb)
        main.postDelayed({ stopScan() }, 12_000)
    }

    fun stopScan() {
        if (!scanning) return
        scanning = false
        try { adapter?.bluetoothLeScanner?.stopScan(scanCb) } catch (_: Exception) {}
        if (phase == Phase.Scanning) {
            phase = Phase.Idle
            detail = if (found.isEmpty())
                "Nie znaleziono bramki. Wgraj firmware Watahy na Heltec V4 i trzymaj moduł blisko."
            else "Znaleziono ${found.size} — wpisz PIN z OLED i sparuj."
        }
        bump()
    }

    fun connect(ctx: Context, address: String, pin: String) {
        val clean = pin.filter { it.isDigit() }
        if (clean.length != 6) {
            phase = Phase.Error
            detail = "PIN musi mieć 6 cyfr z wyświetlacza OLED."
            bump(); return
        }
        stopScan()
        val bt = adapter ?: (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val dev = try { bt.getRemoteDevice(address) } catch (_: Exception) { null }
        if (dev == null) {
            phase = Phase.Error; detail = "Nieznany adres BLE."; bump(); return
        }
        pendingPin = clean
        pairKey = Security.derivePairKey(clean, address)
        deviceAddress = address
        deviceName = try { dev.name } catch (_: SecurityException) { address }
        phase = Phase.Connecting
        detail = "GATT + weryfikacja PIN…"
        bump()
        gatt?.close()
        gatt = if (android.os.Build.VERSION.SDK_INT >= 23)
            dev.connectGatt(ctx, false, gattCb, BluetoothDevice.TRANSPORT_LE)
        else dev.connectGatt(ctx, false, gattCb)
        armPairTimeout()
    }

    fun disconnect() {
        cancelPairTimeout()
        try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}
        gatt = null
        rxChar = null
        phase = Phase.Idle
        detail = "Rozłączono bramkę LoRa."
        deviceName = null
        deviceAddress = null
        pairKey = null
        pendingPin = null
        bump()
    }

    fun sendText(body: String, kind: String = "msg"): Boolean {
        val key = pairKey
        if (phase != Phase.Linked || key == null) {
            note("Brak łącza LoRa — ramka w kolejce store-and-forward.")
            return false
        }
        val ts = System.currentTimeMillis()
        val payload = JSONObject()
            .put("v", 1)
            .put("t", "tx")
            .put("k", kind)
            .put("body", body)
            .put("ts", ts)
        payload.put("hmac", Security.hmacHex(key, body + ts))
        return writeLine(payload.toString())
    }

    fun sendCrisis(kind: String, body: String): Boolean = sendText("$kind|$body", "crisis")

    private fun writeLine(line: String): Boolean {
        val g = gatt ?: return false
        val ch = rxChar ?: return false
        val bytes = (line.trim() + "\n").toByteArray(Charsets.UTF_8)
        return try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                g.writeCharacteristic(ch, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                ch.value = bytes
                @Suppress("DEPRECATION")
                ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                g.writeCharacteristic(ch)
            }
        } catch (e: Exception) {
            note("TX błąd: ${e.message}")
            false
        }
    }

    private fun armPairTimeout() {
        cancelPairTimeout()
        val r = Runnable {
            if (phase == Phase.Connecting) {
                phase = Phase.Error
                detail = "Moduł nie potwierdził PIN (timeout). Wgraj firmware Watahy 0.4."
                note("Timeout parowania PIN.")
                try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}
                gatt = null
                rxChar = null
                pairKey = null
                pendingPin = null
                bump()
            }
        }
        pairTimeout = r
        main.postDelayed(r, 12_000)
    }

    private fun cancelPairTimeout() {
        pairTimeout?.let { main.removeCallbacks(it) }
        pairTimeout = null
    }

    private val scanCb = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.scanRecord?.deviceName ?: result.device.name ?: ""
            val addr = result.device.address ?: return
            val uuids = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
            if (!looksLikeGateway(name) && NUS_SERVICE !in uuids) return
            val label = name.ifBlank { "Heltec V4" }
            val existing = found.indexOfFirst { it.address == addr }
            val row = LoraFound(addr, label, result.rssi)
            if (existing >= 0) found[existing] = row else found += row
            bump()
        }
        override fun onScanFailed(errorCode: Int) {
            phase = Phase.Error
            detail = "Skan BLE nieudany (kod $errorCode)."
            bump()
        }
    }

    private fun looksLikeGateway(name: String): Boolean {
        val n = name.uppercase()
        return n.startsWith("WATAHA") || n.contains("HELTEC") || n.contains("LORA") || n.contains("WIFI LORA")
    }

    private val gattCb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                cancelPairTimeout()
                if (phase == Phase.Linked || phase == Phase.Connecting) {
                    phase = Phase.Idle
                    detail = "BLE rozłączone."
                    pairKey = null
                    pendingPin = null
                    bump()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val svc = g.getService(NUS_SERVICE)
            if (svc == null) {
                phase = Phase.Error
                detail = "Brak usługi UART na module. Wgraj firmware Watahy."
                bump(); return
            }
            rxChar = svc.getCharacteristic(NUS_RX)
            val tx = svc.getCharacteristic(NUS_TX)
            if (tx != null) {
                g.setCharacteristicNotification(tx, true)
                val desc = tx.getDescriptor(CCCD)
                if (desc != null) {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        g.writeDescriptor(desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(desc)
                    }
                }
            }
            main.postDelayed({
                writeLine(JSONObject().put("v", 1).put("t", "hello").put("from", "phone").toString())
                writeLine(
                    JSONObject()
                        .put("v", 1)
                        .put("t", "pair")
                        .put("pin", pendingPin ?: "")
                        .toString(),
                )
                note("Wysłano PIN do weryfikacji na module.")
            }, 400)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            onBytes(ch.value)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray) {
            onBytes(value)
        }
    }

    private fun onBytes(value: ByteArray?) {
        if (value == null) return
        rxBuf.append(String(value, Charsets.UTF_8))
        var nl = rxBuf.indexOf("\n")
        while (nl >= 0) {
            val line = rxBuf.substring(0, nl).trim()
            rxBuf.delete(0, nl + 1)
            if (line.isNotEmpty()) handleLine(line)
            nl = rxBuf.indexOf("\n")
        }
    }

    private fun handleLine(line: String) {
        try {
            val o = JSONObject(line)
            when (o.optString("t")) {
                "hello" -> {
                    firmware = o.optString("fw")
                    band = o.optString("band", band)
                    deviceName = o.optString("dev", deviceName)
                    note("HELLO ${deviceName ?: ""} fw=${firmware ?: "?"} ${band}")
                }
                "paired" -> {
                    cancelPairTimeout()
                    if (o.optBoolean("ok")) {
                        phase = Phase.Linked
                        detail = "Połączono z ${deviceName ?: "Heltec V4"} · PIN OK · EU868 SX1262"
                        note("PIN zaakceptowany. Łącze LoRa aktywne.")
                    } else {
                        val reason = o.optString("reason")
                        phase = Phase.Error
                        detail = when (reason) {
                            "locked" -> "Zbyt wiele błędnych PIN — moduł zablokowany na 5 min."
                            else -> "Zły PIN. Sprawdź 6 cyfr na OLED Heltec."
                        }
                        pairKey = null
                        pendingPin = null
                        note("PIN odrzucony ($reason).")
                        try { gatt?.disconnect() } catch (_: Exception) {}
                    }
                }
                "txok" -> {
                    val ok = o.optBoolean("ok")
                    note(if (ok) "TX LoRa OK" else "TX odrzucony (${o.optString("reason")})")
                }
                "rx" -> {
                    lastRssi = if (o.has("rssi")) o.getInt("rssi") else lastRssi
                    val body = o.optString("body")
                    val hmac = o.optString("hmac")
                    val key = pairKey
                    val ok = key == null || hmac.isBlank() || Security.hmacOk(key, body + o.optLong("ts"), hmac)
                    note("RX rssi=${lastRssi ?: "?"} ${if (ok) body else "HMAC ODRZUCONY"}")
                }
                "pong" -> note("PONG")
                else -> note(line)
            }
        } catch (_: Exception) {
            note(line)
        }
        bump()
    }

    private fun note(s: String) {
        log.add(0, "${System.currentTimeMillis() % 100000}  ${Security.clipLog(s)}")
        if (log.size > 80) log.removeAt(log.lastIndex)
    }
}
