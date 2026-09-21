package app.siatka.wilk

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Trwała, wyłącznie lokalna pamięć adaptacji WILKA.
 * Nie wymaga internetu i nie wysyła danych na serwer.
 */
class SharedPrefsWilkLearningStore(context: Context) : WilkLearningStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("wilk_learning_v1", Context.MODE_PRIVATE)

    override fun read(topic: String): TopicLearning? {
        val raw = prefs.getString(key(topic), null) ?: return null
        return runCatching { decode(raw) }.getOrNull()
    }

    override fun write(state: TopicLearning) {
        prefs.edit().putString(key(state.topic), encode(state)).apply()
    }

    override fun all(): List<TopicLearning> =
        prefs.all.keys
            .asSequence()
            .filter { it.startsWith(PREFIX) }
            .mapNotNull { k -> prefs.getString(k, null)?.let { runCatching { decode(it) }.getOrNull() } }
            .toList()

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun key(topic: String): String = PREFIX + topic

    private fun encode(state: TopicLearning): String {
        val helpful = JSONObject()
        state.helpfulByStyle.forEach { (style, count) -> helpful.put(style.name, count) }

        val notHelpful = JSONObject()
        state.notHelpfulByStyle.forEach { (style, count) -> notHelpful.put(style.name, count) }

        val phrases = JSONArray()
        state.learnedPhrases.take(40).forEach { phrases.put(it) }

        return JSONObject()
            .put("topic", state.topic)
            .put("preferredStyle", state.preferredStyle.name)
            .put("helpful", helpful)
            .put("notHelpful", notHelpful)
            .put("phrases", phrases)
            .toString()
    }

    private fun decode(raw: String): TopicLearning {
        val o = JSONObject(raw)
        val helpful = mutableMapOf<ExplanationStyle, Int>()
        val notHelpful = mutableMapOf<ExplanationStyle, Int>()

        val h = o.optJSONObject("helpful") ?: JSONObject()
        ExplanationStyle.values().forEach { style ->
            if (h.has(style.name)) helpful[style] = h.optInt(style.name, 0)
        }

        val nh = o.optJSONObject("notHelpful") ?: JSONObject()
        ExplanationStyle.values().forEach { style ->
            if (nh.has(style.name)) notHelpful[style] = nh.optInt(style.name, 0)
        }

        val phrases = mutableSetOf<String>()
        val arr = o.optJSONArray("phrases") ?: JSONArray()
        for (i in 0 until arr.length()) {
            arr.optString(i).trim().takeIf { it.isNotBlank() }?.let { phrases += it }
        }

        val style = runCatching {
            ExplanationStyle.valueOf(o.optString("preferredStyle", ExplanationStyle.STANDARD.name))
        }.getOrDefault(ExplanationStyle.STANDARD)

        return TopicLearning(
            topic = o.getString("topic"),
            preferredStyle = style,
            helpfulByStyle = helpful,
            notHelpfulByStyle = notHelpful,
            learnedPhrases = phrases
        )
    }

    companion object {
        private const val PREFIX = "topic::"
    }
}

/** Fabryka używana przez aplikację docelową przy integracji. */
fun createPersistentWilk(
    context: Context,
    loraRegistry: LoraSupportRegistry = NoConfirmedLoraSupport
): WilkAdaptiveCore =
    WilkAdaptiveCore(
        store = SharedPrefsWilkLearningStore(context),
        loraRegistry = loraRegistry
    )
