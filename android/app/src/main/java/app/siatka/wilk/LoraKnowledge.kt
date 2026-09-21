package app.siatka.wilk

/**
 * Warstwa wiedzy LoRa dla WILKA.
 *
 * Kluczowa zasada:
 * WILK nie może nazywać sprzętu "obsługiwanym", dopóki finalna Polska Wataha
 * nie potwierdzi go przez rejestr działającego hardware support.
 */
data class LoraDeviceSupport(
    val id: String,
    val displayName: String,
    val connection: String,
    val regionalProfile: String,
    val tested: Boolean
)

interface LoraSupportRegistry {
    fun supportedDevices(): List<LoraDeviceSupport>
}

object NoConfirmedLoraSupport : LoraSupportRegistry {
    override fun supportedDevices(): List<LoraDeviceSupport> = emptyList()
}

object LoraKnowledge {
    data class LoraReply(val topic: String, val text: String)

    private fun normalize(s: String): String {
        val nf = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return nf.replace("\\p{Mn}+".toRegex(), "").replace('ł', 'l').replace('Ł', 'L').lowercase()
    }

    fun answer(
        userText: String,
        registry: LoraSupportRegistry = NoConfirmedLoraSupport
    ): LoraReply? {
        val q = normalize(userText)
        val mentionsLora = listOf(
            "lora", "sx1262", "e22", "heltec", "meshtastic",
            "868 mhz", "eu868", "modul radiowy"
        ).any { q.contains(it) }
        if (!mentionsLora) return null

        val devices = registry.supportedDevices().filter { it.tested }

        val wantsToBuy = listOf(
            "jaki kupic", "co kupic", "jaki modul", "nie mam modulu",
            "polecisz", "polec", "wybrac", "najlepszy"
        ).any { q.contains(it) }

        if (wantsToBuy) {
            if (devices.isEmpty()) {
                return LoraReply(
                    "LoRa — wybór sprzętu",
                    """
                    📡 Nie mam jeszcze potwierdzonej listy urządzeń LoRa przetestowanych z finalną Polską Watahą.
                    Dlatego nie będę udawał, że konkretny model na pewno zadziała.

                    Przy wyborze sprawdzaj:
                    • zgodność z profilem regionalnym używanym w Polsce/UE,
                    • sposób połączenia obsługiwany przez aplikację,
                    • czy model jest oznaczony w Polskiej Watasze jako PRZETESTOWANY,
                    • czy aplikacja ma dla niego działający sterownik.

                    Gdy finalna aplikacja przekaże mi listę wspieranego sprzętu, będę mógł dobrać konkretny model do budżetu, prostoty obsługi i zastosowania.
                    """.trimIndent()
                )
            }

            val list = devices.take(5).joinToString("\n") {
                "• ${it.displayName} — ${it.connection}, ${it.regionalProfile}"
            }
            return LoraReply(
                "LoRa — wybór sprzętu",
                "📡 Urządzenia potwierdzone przez finalną Polską Watahę:\n$list\n\nWybieraj tylko model oznaczony jako przetestowany w aplikacji."
            )
        }

        val wantsSetup = listOf(
            "polaczyc", "sparowac", "skonfigurowac", "ustawic",
            "telefon", "usb", "otg", "bluetooth", "nie widzi", "nie dziala"
        ).any { q.contains(it) }

        if (wantsSetup) {
            val supportNote = if (devices.isEmpty()) {
                "Najpierw sprawdź w aplikacji, czy Twój model jest oznaczony jako przetestowany."
            } else {
                "Przetestowane modele: " + devices.take(5).joinToString { it.displayName } + "."
            }

            return LoraReply(
                "LoRa — konfiguracja",
                """
                📡 Konfiguracja LoRa:
                1. $supportNote
                2. Podłącz moduł metodą przewidzianą dla danego modelu.
                3. Nadaj aplikacji wymagane uprawnienia.
                4. Otwórz Łączność → LoRa i uruchom wykrywanie.
                5. Ustaw profil regionalny zgodny z finalną konfiguracją aplikacji.
                6. Wykonaj realny test nadawania i odbioru z drugim węzłem.
                7. Jeżeli test nie przejdzie, sprawdź kolejno: zasilanie → przewód/łączność → uprawnienia → firmware → zgodność modelu.

                Nie uznawaj konfiguracji za działającą tylko dlatego, że moduł został wykryty.
                """.trimIndent()
            )
        }

        return LoraReply(
            "LoRa — informacje",
            """
            📡 LoRa może służyć Polskiej Watasze do krótkich wiadomości i łączności awaryjnej poza internetem.
            Telefon zwykle potrzebuje do tego zgodnego zewnętrznego modułu.
            Konkretne instrukcje sprzętowe WILK podaje dopiero na podstawie listy urządzeń faktycznie przetestowanych przez finalną aplikację.
            """.trimIndent()
        )
    }
}
