package app.siatka.wilk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WilkAdaptiveCoreTest {
    @Test
    fun `prosba o prostsze wyjasnienie nie zmienia tematu odpowiedzi`() {
        val core = WilkAdaptiveCore()
        val first = core.ask("Jak rozpalić ogień?")
        val second = core.ask("Nie rozumiem, wyjaśnij prościej")
        assertEquals("Ogień", first.topic)
        assertEquals("Ogień", second.topic)
        assertEquals(ExplanationStyle.SIMPLE, second.style)
        assertTrue(second.text.contains("Najprościej"))
    }

    @Test
    fun `wyjasnij inaczej przechodzi do innego stylu`() {
        val core = WilkAdaptiveCore()
        core.ask("Jak oczyścić wodę?")
        val next = core.explainDifferently()
        assertNotNull(next)
        assertEquals(ExplanationStyle.SIMPLE, next!!.style)
    }

    @Test
    fun `feedback uczy preferowanego stylu ale nie tresci procedury`() {
        val store = InMemoryWilkLearningStore()
        val core = WilkAdaptiveCore(store)
        core.ask("Jak rozpalić ogień?")
        val simple = core.ask("napisz prościej")
        core.feedback(simple.id, FeedbackRating.HELPFUL, "tak jest zrozumiale")
        val state = core.getLearningState("Ogień")
        assertNotNull(state)
        assertEquals(ExplanationStyle.SIMPLE, state!!.preferredStyle)
        assertFalse(state.learnedPhrases.contains("tak jest zrozumiale"))
    }

    @Test
    fun `wilk rozpoznaje pytanie o zakup lora`() {
        val core = WilkAdaptiveCore()
        val answer = core.ask("Nie mam modułu LoRa. Jaki moduł kupić do telefonu?")
        assertEquals("LoRa — wybór sprzętu", answer.topic)
        assertTrue(answer.text.contains("potwierdzonej") || answer.text.contains("przetestowany"))
    }

    @Test
    fun `wilk prowadzi konfiguracje lora`() {
        val core = WilkAdaptiveCore()
        val answer = core.ask("Jak połączyć moduł LoRa z telefonem przez USB OTG?")
        assertEquals("LoRa — konfiguracja", answer.topic)
        assertTrue(answer.text.contains("zgodn") || answer.text.contains("Podłącz"))
    }

    @Test
    fun `nauczone sformulowanie wraca do zatwierdzonego tematu`() {
        val store = InMemoryWilkLearningStore()
        val core = WilkAdaptiveCore(store)
        core.learnPhrase("Woda", "co mam zrobić z tą mętną wodą")
        val answer = core.ask("co mam zrobić z tą mętną wodą")
        assertEquals("Woda", answer.topic)
        assertEquals("learned-phrase->local-kb", answer.source)
    }

    @Test
    fun `odpowiedz kryzysowa nie jest przepisywana stylem`() {
        val core = WilkAdaptiveCore()
        val first = core.ask("Nie oddycha, co robić?")
        val second = core.ask("napisz prościej")
        assertTrue(first.crisis)
        assertTrue(second.crisis)
        assertEquals(first.text, second.text)
        assertEquals(ExplanationStyle.STANDARD, second.style)
    }

    @Test
    fun `rko korzysta z warstwy zweryfikowanej i sugeruje akcje kryzysowe`() {
        val core = WilkAdaptiveCore()
        val answer = core.ask("Nie oddycha, potrzebuję RKO")
        assertEquals("verified-crisis-kb", answer.source)
        assertTrue(answer.crisis)
        assertTrue(answer.text.contains("100–120"))
        assertTrue(answer.suggestedActions.any { it.action == WilkAction.OPEN_CRISIS_MODE })
        assertTrue(answer.suggestedActions.any { it.action == WilkAction.CALL_112 })
    }

    @Test
    fun `rejestr lora pokazuje tylko przetestowany sprzet`() {
        val registry = object : LoraSupportRegistry {
            override fun supportedDevices(): List<LoraDeviceSupport> = listOf(
                LoraDeviceSupport("ok", "Test868", "USB", "EU868", tested = true),
                LoraDeviceSupport("no", "Untested", "USB", "EU868", tested = false)
            )
        }
        val core = WilkAdaptiveCore(InMemoryWilkLearningStore(), registry)
        val answer = core.ask("Jaki moduł LoRa kupić?")
        assertTrue(answer.text.contains("Test868"))
        assertFalse(answer.text.contains("Untested"))
    }

    @Test
    fun `target registry potwierdza Heltec V4`() {
        val core = WilkAdaptiveCore(InMemoryWilkLearningStore(), TargetLoraRegistry)
        val answer = core.ask("Jaki moduł LoRa kupić?")
        assertTrue(answer.text.contains("Heltec WiFi LoRa 32 V4"))
        assertTrue(answer.text.contains("EU868"))
    }

    @Test
    fun `nowe pytanie z fraza krok po kroku zmienia temat zamiast przerabiac poprzedni`() {
        val core = WilkAdaptiveCore(InMemoryWilkLearningStore(), TargetLoraRegistry)
        core.ask("Jak oczyścić wodę?")
        val answer = core.ask("Krok po kroku jak skonfigurować LoRa?")
        assertEquals("LoRa — konfiguracja", answer.topic)
        assertEquals(ExplanationStyle.STEP_BY_STEP, answer.style)
    }

    @Test
    fun `mapa sugeruje integracje z mapa offline`() {
        val core = WilkAdaptiveCore()
        val answer = core.ask("gdzie jestem, pokaż mapę")
        assertTrue(answer.suggestedActions.any { it.action == WilkAction.OPEN_OFFLINE_MAP })
        assertTrue(answer.suggestedActions.any { it.action == WilkAction.SHOW_MY_LOCATION })
        assertFalse(answer.crisis)
    }
}

// CI synchronization marker for WILK validation.

// target CI trigger
