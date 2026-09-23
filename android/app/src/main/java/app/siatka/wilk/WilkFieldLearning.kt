package app.siatka.wilk

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pamięć doświadczeń terenowych WILKA.
 *
 * To NIE jest samomodyfikująca się baza faktów. Użytkownik może zgłosić:
 * - co WILK zasugerował,
 * - co faktycznie zrobił,
 * - czy to zadziałało,
 * - w jakich warunkach.
 *
 * Pojedyncza obserwacja pozostaje doświadczeniem użytkownika i nie może
 * nadpisywać VerifiedCrisisKnowledge ani podpisanych paczek wiedzy.
 */
data class FieldObservation(
    val id: String,
    val topic: String,
    val proposedAction: String?,
    val actionTaken: String,
    val worked: Boolean,
    val context: String,
    val createdAtEpochMs: Long
)

interface WilkFieldLearningStore {
    fun add(observation: FieldObservation)
    fun forTopic(topic: String): List<FieldObservation>
    fun all(): List<FieldObservation>
}

class InMemoryWilkFieldLearningStore : WilkFieldLearningStore {
    private val data = mutableListOf<FieldObservation>()

    override fun add(observation: FieldObservation) {
        data += observation
        if (data.size > 250) data.removeAt(0)
    }

    override fun forTopic(topic: String): List<FieldObservation> =
        data.filter { it.topic == topic }.takeLast(40)

    override fun all(): List<FieldObservation> = data.toList()
}

class SharedPrefsWilkFieldLearningStore(context: Context) : WilkFieldLearningStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("wilk_field_learning_v1", Context.MODE_PRIVATE)

    override fun add(observation: FieldObservation) {
        val current = all().toMutableList()
        current += observation
        val trimmed = current.takeLast(250)
        prefs.edit().putString(KEY, encode(trimmed)).apply()
    }

    override fun forTopic(topic: String): List<FieldObservation> =
        all().filter { it.topic == topic }.takeLast(40)

    override fun all(): List<FieldObservation> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { decode(raw) }.getOrDefault(emptyList())
    }

    private fun encode(items: List<FieldObservation>): String {
        val arr = JSONArray()
        items.forEach { x ->
            arr.put(
                JSONObject()
                    .put("id", x.id)
                    .put("topic", x.topic)
                    .put("proposedAction", x.proposedAction)
                    .put("actionTaken", x.actionTaken)
                    .put("worked", x.worked)
                    .put("context", x.context)
                    .put("createdAtEpochMs", x.createdAtEpochMs)
            )
        }
        return arr.toString()
    }

    private fun decode(raw: String): List<FieldObservation> {
        val arr = JSONArray(raw)
        val out = mutableListOf<FieldObservation>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += FieldObservation(
                id = o.getString("id"),
                topic = o.getString("topic"),
                proposedAction = o.optString("proposedAction").takeIf { it.isNotBlank() && it != "null" },
                actionTaken = o.getString("actionTaken"),
                worked = o.optBoolean("worked", false),
                context = o.optString("context", ""),
                createdAtEpochMs = o.optLong("createdAtEpochMs", 0L)
            )
        }
        return out
    }

    companion object {
        private const val KEY = "observations"
    }
}

/**
 * Wnioski są osobiste i opisowe. Nie stają się automatycznie "prawdą survivalową".
 */
object WilkFieldLearning {
    fun summarize(topic: String, store: WilkFieldLearningStore): List<String> {
        val observations = store.forTopic(topic)
        if (observations.isEmpty()) return emptyList()

        return observations
            .filter { it.worked }
            .groupBy { normalize(it.actionTaken) }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { (_, entries) ->
                val latest = entries.maxByOrNull { it.createdAtEpochMs }!!
                val count = entries.size
                if (count == 1) {
                    "Wcześniej zgłosiłeś, że zadziałało: ${latest.actionTaken}" +
                        latest.context.takeIf { it.isNotBlank() }?.let { " (warunki: $it)" }.orEmpty()
                } else {
                    "Zgłaszałeś $count razy, że działało: ${latest.actionTaken}" +
                        latest.context.takeIf { it.isNotBlank() }?.let { " (ostatnie warunki: $it)" }.orEmpty()
                }
            }
    }

    private fun normalize(s: String): String =
        s.lowercase().trim().replace(Regex("\\s+"), " ")
}
