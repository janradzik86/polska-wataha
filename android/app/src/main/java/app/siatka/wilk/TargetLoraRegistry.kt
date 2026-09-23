package app.siatka.wilk

object TargetLoraRegistry : LoraSupportRegistry {
    override fun supportedDevices(): List<LoraDeviceSupport> = listOf(
        LoraDeviceSupport(
            id = "heltec-v4-eu868",
            displayName = "Heltec WiFi LoRa 32 V4",
            connection = "Bluetooth LE / Nordic UART",
            regionalProfile = "EU868",
            tested = true
        )
    )
}