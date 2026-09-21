package app.siatka.wilk

/**
 * WILK vNext — adaptacyjna warstwa nad lokalnym silnikiem wiedzy.
 *
 * Zasady:
 * - fakty i procedury pochodzą wyłącznie z lokalnej, zatwierdzonej bazy,
 * - "uczenie" zmienia sposób tłumaczenia i rozpoznawanie sformułowań, nie fakty,
 * - odpowiedzi kryzysowe są zablokowane przed swobodnym przepisywaniem,
 * - brak internetu nie blokuje działania.
 */
enum class ExplanationStyle { STANDARD, SIMPLE, STEP_BY_STEP, TECHNICAL }

enum class FeedbackRating { HELPFUL, NOT_HELPFUL }

data class WilkContext(
    val preferredStyle: ExplanationStyle = ExplanationStyle.STANDARD,
    val lastTopic: String? = null,
    val lastAnswerId: String? = null
)

data class WilkAnswer(
    val id: String,
    val text: String,
    val topic: String?,
    val crisis: Boolean,
    val style: ExplanationStyle,
    val source: String = "verified-local-kb",
    val suggestedActions: List<WilkActionSuggestion> = emptyList()
)

data class TopicLearning(
    val topic: String,
    val preferredStyle: ExplanationStyle = ExplanationStyle.STANDARD,
    val helpfulByStyle: Map<ExplanationStyle, Int> = emptyMap(),
    val notHelpfulByStyle: Map<ExplanationStyle, Int> = emptyMap(),
    val learnedPhrases: Set<String> = emptySet()
)

interface WilkLearningStore {
    fun read(topic: String): TopicLearning?
    fun write(state: TopicLearning)
    fun all(): List<TopicLearning>
}

class InMemoryWilkLearningStore : WilkLearningStore {
    private val states = mutableMapOf<String, TopicLearning>()
    override fun read(topic: String): TopicLearning? = states[topic]
    override fun write(state: TopicLearning) { states[state.topic] = state }
    override fun all(): List<TopicLearning> = states.values.toList()
}

/**
 * Główny punkt wejścia dla finalnego WILKA.
 * Docelowe UI powinno rozmawiać z tą klasą, nie bezpośrednio z AssistantEngine.
 */
class WilkAdaptiveCore(
    private val store: WilkLearningStore = InMemoryWilkLearningStore(),
    private val loraRegistry: LoraSupportRegistry = NoConfirmedLoraSupport
) {
    private var lastAnswer: WilkAnswer? = null

    fun ask(text: String, context: WilkContext = WilkContext()): WilkAnswer {
        val normalized = normalize(text)
        val requestedStyle = detectRequestedStyle(normalized)

        // Po odpowiedzi kryzysowej nie robimy swobodnego "przepisz inaczej".
        if (requestedStyle != null && lastAnswer != null && isStyleOnly(normalized)) {
            val previous = lastAnswer!!
            if (previous.crisis) return previous

            val rewritten = ExplanationLibrary.rewrite(previous.topic, previous.text, requestedStyle)
            val result = previous.copy(
                id = answerId(previous.topic, requestedStyle, rewritten),
                text = rewritten,
                style = requestedStyle
            )
            rememberStyle(previous.topic, requestedStyle)
            lastAnswer = result
            return result
        }

        val verified = VerifiedCrisisKnowledge.answer(text)
        val lora = if (verified == null) LoraKnowledge.answer(text, loraRegistry) else null
        val learnedTopic = if (verified == null && lora == null) resolveLearnedTopic(normalized) else null

        val base: AssistantEngine.Reply
        val source: String

        when {
            verified != null -> {
                base = AssistantEngine.Reply(
                    text = verified.text,
                    crisis = verified.crisis,
                    topic = verified.topic
                )
                source = "verified-crisis-kb"
            }
            lora != null -> {
                base = AssistantEngine.Reply(lora.text, crisis = false, topic = lora.topic)
                source = "verified-lora-kb"
            }
            learnedTopic != null -> {
                val entry = SurvivalData.KB.firstOrNull { it.topic == learnedTopic }
                if (entry != null) {
                    base = AssistantEngine.Reply(entry.answer, crisis = false, topic = entry.topic)
                    source = "learned-phrase->local-kb"
                } else {
                    base = AssistantEngine.answer(text)
                    source = "legacy-local-kb"
                }
            }
            else -> {
                base = AssistantEngine.answer(text)
                source = "legacy-local-kb"
            }
        }

        // Zapamiętujemy realne sformułowanie pytania jako synonim danego tematu.
        if (!base.crisis && base.topic != null && requestedStyle == null) {
            rememberPhrase(base.topic, normalized)
        }

        val learnedStyle = base.topic?.let { store.read(it)?.preferredStyle }
        val style = if (base.crisis) {
            ExplanationStyle.STANDARD
        } else {
            requestedStyle ?: learnedStyle ?: context.preferredStyle
        }

        val rendered = if (base.crisis || style == ExplanationStyle.STANDARD) {
            base.text
        } else {
            ExplanationLibrary.rewrite(base.topic, base.text, style)
        }

        val answer = WilkAnswer(
            id = answerId(base.topic, style, rendered),
            text = rendered,
            topic = base.topic,
            crisis = base.crisis,
            style = style,
            source = source,
            suggestedActions = WilkIntegrationContract.actionsFor(base.topic, base.crisis)
        )
        lastAnswer = answer
        return answer
    }

    fun explainDifferently(): WilkAnswer? {
        val previous = lastAnswer ?: return null
        if (previous.crisis) return previous

        val nextStyle = when (previous.style) {
            ExplanationStyle.STANDARD -> ExplanationStyle.SIMPLE
            ExplanationStyle.SIMPLE -> ExplanationStyle.STEP_BY_STEP
            ExplanationStyle.STEP_BY_STEP -> ExplanationStyle.TECHNICAL
            ExplanationStyle.TECHNICAL -> ExplanationStyle.SIMPLE
        }
        val rewritten = ExplanationLibrary.rewrite(previous.topic, previous.text, nextStyle)
        val result = previous.copy(
            id = answerId(previous.topic, nextStyle, rewritten),
            text = rewritten,
            style = nextStyle
        )
        rememberStyle(previous.topic, nextStyle)
        lastAnswer = result
        return result
    }

    fun feedback(answerId: String, rating: FeedbackRating, userComment: String? = null) {
        val answer = lastAnswer ?: return
        if (answer.id != answerId || answer.crisis) return
        val topic = answer.topic ?: return

        val current = store.read(topic) ?: TopicLearning(topic)
        val helpful = current.helpfulByStyle.toMutableMap()
        val notHelpful = current.notHelpfulByStyle.toMutableMap()

        when (rating) {
            FeedbackRating.HELPFUL -> helpful[answer.style] = (helpful[answer.style] ?: 0) + 1
            FeedbackRating.NOT_HELPFUL -> notHelpful[answer.style] = (notHelpful[answer.style] ?: 0) + 1
        }

        // Komentarz użytkownika może być wykorzystany przez UI/telemetrię lokalną,
        // ale NIE zapisujemy go jako synonimu tematu. Dzięki temu tekst typu
        // "tak jest zrozumiale" nie zacznie później kierować pytań do losowej procedury.
        userComment?.trim()

        val preferred = choosePreferredStyle(helpful, notHelpful, current.preferredStyle)
        store.write(
            current.copy(
                preferredStyle = preferred,
                helpfulByStyle = helpful,
                notHelpfulByStyle = notHelpful
            )
        )
    }

    /** Jawne uczenie lokalnego synonimu/sformułowania dla zatwierdzonego tematu. */
    fun learnPhrase(topic: String, phrase: String) {
        val normalized = normalize(phrase).trim()
        if (normalized.length !in 3..160) return
        val knownTopic = SurvivalData.KB.any { it.topic == topic } ||
            topic.startsWith("LoRa") ||
            VerifiedCrisisKnowledge.knownTopics.contains(topic)
        if (!knownTopic) return
        rememberPhrase(topic, normalized)
    }

    fun getLearningState(topic: String): TopicLearning? = store.read(topic)

    private fun rememberStyle(topic: String?, style: ExplanationStyle) {
        if (topic == null) return
        val current = store.read(topic) ?: TopicLearning(topic)
        store.write(current.copy(preferredStyle = style))
    }

    private fun rememberPhrase(topic: String, normalizedPhrase: String) {
        if (normalizedPhrase.length !in 3..160) return
        if (isStyleCommand(normalizedPhrase)) return

        val current = store.read(topic) ?: TopicLearning(topic)
        val phrases = current.learnedPhrases.toMutableSet()
        phrases += normalizedPhrase
        // Ograniczamy rozrost lokalnej pamięci na temat.
        val trimmed = phrases.toList().takeLast(40).toSet()
        store.write(current.copy(learnedPhrases = trimmed))
    }

    private fun resolveLearnedTopic(q: String): String? {
        if (q.length < 3) return null

        return store.all()
            .asSequence()
            .flatMap { state -> state.learnedPhrases.asSequence().map { phrase -> state.topic to phrase } }
            .filter { (_, phrase) -> phrase.length >= 3 }
            .map { (topic, phrase) ->
                val score = when {
                    q == phrase -> 1000 + phrase.length
                    q.contains(phrase) -> 500 + phrase.length
                    phrase.contains(q) && q.length >= 6 -> 250 + q.length
                    else -> tokenOverlapScore(q, phrase)
                }
                topic to score
            }
            .filter { (_, score) -> score >= 30 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun tokenOverlapScore(a: String, b: String): Int {
        val ta = a.split(Regex("\\s+")).filter { it.length >= 4 }.toSet()
        val tb = b.split(Regex("\\s+")).filter { it.length >= 4 }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0
        return ta.intersect(tb).sumOf { it.length * 5 }
    }

    private fun choosePreferredStyle(
        helpful: Map<ExplanationStyle, Int>,
        notHelpful: Map<ExplanationStyle, Int>,
        fallback: ExplanationStyle
    ): ExplanationStyle {
        return ExplanationStyle.values().maxByOrNull { style ->
            (helpful[style] ?: 0) * 2 - (notHelpful[style] ?: 0)
        }?.takeIf { (helpful[it] ?: 0) > 0 } ?: fallback
    }

    private fun detectRequestedStyle(q: String): ExplanationStyle? = when {
        listOf("nie rozumiem", "prosciej", "latwiej", "po ludzku", "wytlumacz prosto", "nie da sie tak", "nie kumam")
            .any { q.contains(it) } -> ExplanationStyle.SIMPLE
        listOf("krok po kroku", "po kolei", "co najpierw", "instrukcja krok")
            .any { q.contains(it) } -> ExplanationStyle.STEP_BY_STEP
        listOf("technicznie", "dokladniej", "szczegolowo", "parametry")
            .any { q.contains(it) } -> ExplanationStyle.TECHNICAL
        else -> null
    }

    private fun isStyleCommand(q: String): Boolean = detectRequestedStyle(q) != null

    private fun isStyleOnly(q: String): Boolean {
        var rest = q
        val phrases = listOf(
            "nie rozumiem", "prosciej", "latwiej", "po ludzku", "wytlumacz prosto",
            "nie da sie tak", "nie kumam", "krok po kroku", "po kolei", "co najpierw",
            "instrukcja krok", "technicznie", "dokladniej", "szczegolowo", "parametry"
        )
        phrases.forEach { rest = rest.replace(it, " ") }
        val filler = setOf("prosze", "wytlumacz", "napisz", "pokaz", "mi", "to", "jeszcze", "raz", "teraz")
        val meaningful = rest
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 && it !in filler }
        return meaningful.size <= 1
    }

    private fun answerId(topic: String?, style: ExplanationStyle, text: String): String {
        val hash = (topic.orEmpty() + "|" + style.name + "|" + text).hashCode()
        return "wilk-" + hash.toUInt().toString(16)
    }

    private fun normalize(s: String): String {
        val nf = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return nf.replace("\\p{Mn}+".toRegex(), "").replace('ł', 'l').replace('Ł', 'L').lowercase().trim()
    }
}

object ExplanationLibrary {
    fun rewrite(topic: String?, original: String, style: ExplanationStyle): String {
        if (style == ExplanationStyle.STANDARD) return original
        return when (topic) {
            "Ogień" -> fire(style)
            "Woda" -> water(style)
            "LoRa — wybór sprzętu" -> loraBuy(style)
            "LoRa — konfiguracja" -> loraSetup(style)
            else -> generic(original, style)
        }
    }

    private fun fire(style: ExplanationStyle): String = when (style) {
        ExplanationStyle.SIMPLE ->
            "🔥 Najprościej: przygotuj rozpałkę, cienkie suche patyczki i grubsze drewno. Najpierw zapal rozpałkę, potem stopniowo dokładaj cienkie patyczki, a grubsze drewno dopiero gdy płomień jest stabilny. Ogień rozpalaj wyłącznie w bezpiecznym miejscu i nigdy bez wentylacji w zamkniętym pomieszczeniu."
        ExplanationStyle.STEP_BY_STEP ->
            "🔥 Ogień krok po kroku:\n1. Wybierz bezpieczne miejsce z dala od materiałów łatwopalnych.\n2. Przygotuj suchą rozpałkę.\n3. Przygotuj osobno cienkie patyczki i grubsze drewno.\n4. Zapal rozpałkę.\n5. Gdy płomień jest stabilny, dodawaj cienkie patyczki.\n6. Dopiero potem dodawaj grubsze drewno.\n7. Nie zostawiaj ognia bez nadzoru."
        ExplanationStyle.TECHNICAL ->
            "🔥 Ogień potrzebuje paliwa, tlenu i odpowiedniej temperatury. Zwiększaj rozmiar paliwa stopniowo: rozpałka → cienkie patyczki → grubsze drewno. Zbyt duży kawałek dołożony za wcześnie może odebrać ciepło i zdusić płomień. Zachowaj przepływ powietrza i bezpieczne otoczenie."
        ExplanationStyle.STANDARD -> error("handled above")
    }

    private fun water(style: ExplanationStyle): String = when (style) {
        ExplanationStyle.SIMPLE ->
            "💧 Jeśli nie masz pewności, że woda jest bezpieczna, nie pij jej od razu. Wybierz najczystsze dostępne źródło i stosuj oficjalne zalecenia dotyczące uzdatniania. Przy podejrzeniu skażenia chemicznego samo filtrowanie lub gotowanie może nie wystarczyć."
        ExplanationStyle.STEP_BY_STEP ->
            "💧 Woda krok po kroku:\n1. Wybierz najczystsze dostępne źródło.\n2. Usuń widoczne osady filtrem lub czystą tkaniną.\n3. Zastosuj zatwierdzoną metodę dezynfekcji zgodnie z instrukcją lub komunikatem służb.\n4. Przechowuj wodę w czystym, zamkniętym pojemniku.\n5. Przy ostrzeżeniu o skażeniu chemicznym korzystaj z bezpiecznego źródła wskazanego przez służby."
        ExplanationStyle.TECHNICAL ->
            "💧 Filtracja mechaniczna może usuwać zawiesiny, ale nie gwarantuje usunięcia wszystkich drobnoustrojów ani związków chemicznych. Metoda uzdatniania zależy od rodzaju zagrożenia. Przy oficjalnym komunikacie o skażeniu stosuj zalecenia służb i bezpieczne źródło zastępcze."
        ExplanationStyle.STANDARD -> error("handled above")
    }

    private fun loraBuy(style: ExplanationStyle): String = when (style) {
        ExplanationStyle.SIMPLE ->
            "📡 Jeśli jeszcze nic nie kupiłeś, najpierw sprawdź listę urządzeń faktycznie obsługiwanych przez Polską Watahę. Dla Polski/UE sprzęt musi być zgodny z właściwym profilem regionalnym. Nie kupuj modułu tylko dlatego, że jest popularny."
        ExplanationStyle.STEP_BY_STEP ->
            "📡 Dobór LoRa krok po kroku:\n1. Otwórz w aplikacji listę obsługiwanego sprzętu.\n2. Sprawdź sposób połączenia z telefonem.\n3. Sprawdź wariant regionalny urządzenia.\n4. Wybierz sprzęt, dla którego aplikacja ma działający sterownik.\n5. Dopiero wtedy kup konkretny model."
        ExplanationStyle.TECHNICAL ->
            "📡 Warstwa WILKA nie deklaruje konkretnego modułu jako wspieranego bez działającego sterownika i testu transmisji w finalnej Polskiej Watasze. Dobór sprzętu musi być zsynchronizowany z aktualnym Hardware Support Registry aplikacji i profilem regionalnym."
        ExplanationStyle.STANDARD -> error("handled above")
    }

    private fun loraSetup(style: ExplanationStyle): String = when (style) {
        ExplanationStyle.SIMPLE ->
            "📡 Podłącz wyłącznie moduł oznaczony przez Polską Watahę jako obsługiwany. Otwórz Łączność → LoRa, uruchom wykrywanie i wykonaj test połączenia. Jeśli aplikacja go nie widzi, sprawdź zasilanie, przewód lub łączność oraz zgodność modelu."
        ExplanationStyle.STEP_BY_STEP ->
            "📡 Konfiguracja LoRa krok po kroku:\n1. Sprawdź zgodność modułu.\n2. Podłącz go zgodnie z instrukcją sprzętu.\n3. Nadaj wymagane uprawnienia.\n4. Otwórz Łączność → LoRa.\n5. Wykryj urządzenie.\n6. Wybierz właściwy profil regionalny.\n7. Uruchom test nadawania i odbioru.\n8. Zapisz konfigurację dopiero po udanym teście."
        ExplanationStyle.TECHNICAL ->
            "📡 Integracja powinna przejść pełną ścieżkę: wykrycie urządzenia → otwarcie transportu → konfiguracja profilu → test ramki → potwierdzenie odbioru. WILK nie powinien uznawać sprzętu za skonfigurowany bez realnego potwierdzenia transmisji."
        ExplanationStyle.STANDARD -> error("handled above")
    }

    private fun generic(original: String, style: ExplanationStyle): String = when (style) {
        ExplanationStyle.SIMPLE -> "🐺 Prościej:\n" + original.replace(";", ".").replace(" → ", " potem ")
        ExplanationStyle.STEP_BY_STEP -> {
            val parts = original.split(Regex("[.!?]\\s+")).map { it.trim() }.filter { it.isNotBlank() }.take(6)
            if (parts.isEmpty()) original
            else "🐺 Krok po kroku:\n" + parts.mapIndexed { i, p -> (i + 1).toString() + ". " + p }.joinToString("\n")
        }
        ExplanationStyle.TECHNICAL -> "🐺 Szczegółowo:\n" + original
        ExplanationStyle.STANDARD -> original
    }
}
