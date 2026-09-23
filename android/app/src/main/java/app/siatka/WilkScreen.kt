package app.siatka

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.siatka.wilk.*

@Composable
fun WilkScreen(
    activity: MainActivity,
    onMap: () -> Unit,
    onCrisis: () -> Unit,
    onLora: () -> Unit,
    onStatus: () -> Unit,
) {
    val core = remember { createPersistentWilk(activity, TargetLoraRegistry) }
    var input by remember { mutableStateOf("") }
    var answer by remember {
        mutableStateOf(
            WilkAnswer(
                id = "welcome",
                text = "🐺 Jestem WILK. Działam lokalnie i offline. Zapytaj o wodę, ogień, pierwszą pomoc, mapę, powódź, brak prądu albo LoRa.",
                topic = "Powitanie",
                crisis = false,
                style = ExplanationStyle.STANDARD
            )
        )
    }

    fun ask() {
        val q = input.trim()
        if (q.isNotEmpty()) {
            answer = core.ask(q)
            input = ""
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("WILK", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Lokalny asystent kryzysowy · działa bez internetu", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                answer.topic?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                Text(answer.text)
                Text("Źródło: " + answer.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Zapytaj WILKA") },
            minLines = 2
        )
        Button(onClick = { ask() }, modifier = Modifier.fillMaxWidth()) { Text("Zapytaj") }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { core.explainDifferently()?.let { answer = it } },
                modifier = Modifier.weight(1f)
            ) { Text("Wyjaśnij inaczej") }
            OutlinedButton(
                onClick = { core.feedback(answer.id, FeedbackRating.HELPFUL) },
                modifier = Modifier.weight(1f)
            ) { Text("👍 Pomogło") }
        }

        answer.suggestedActions.distinctBy { it.action }.forEach { suggestion ->
            when (suggestion.action) {
                WilkAction.OPEN_CRISIS_MODE -> Button(onClick = onCrisis, modifier = Modifier.fillMaxWidth()) { Text("🚨 Tryb kryzysowy") }
                WilkAction.CALL_112 -> Button(
                    onClick = { activity.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📞 Zadzwoń 112") }
                WilkAction.OPEN_OFFLINE_MAP -> OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth()) { Text("🗺️ Mapa") }
                WilkAction.SHOW_MY_LOCATION -> OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth()) { Text("📍 Moja pozycja") }
                WilkAction.OPEN_LORA, WilkAction.OPEN_MESH -> OutlinedButton(onClick = onLora, modifier = Modifier.fillMaxWidth()) { Text("📡 Radio LoRa / Mesh") }
                WilkAction.OPEN_SURVIVAL_GUIDE -> Text("🏕️ Poradnik jest dostępny przez bazę WILKA.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                WilkAction.CHECK_KNOWLEDGE_UPDATES -> OutlinedButton(
                    onClick = {
                        answer = core.ask("Aktualizacje wiedzy")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔄 Sprawdź aktualizacje wiedzy") }
                WilkAction.OPEN_NAVIGATION -> OutlinedButton(
                    onClick = onMap,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧭 Nawiguj offline") }
            }
        }

        OutlinedButton(onClick = onStatus, modifier = Modifier.fillMaxWidth()) { Text("📶 Status sieci") }
        Text(
            "WILK nie zastępuje numeru 112, ratownika ani lekarza. Przy zagrożeniu życia skorzystaj z oficjalnych służb, jeśli są dostępne.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}