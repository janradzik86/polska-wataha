package app.siatka

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

private val Bg = Color(0xFF0C0808)
private val SurfaceC = Color(0xFF160F0F)
private val Elevated = Color(0xFF1E1414)
private val Fg = Color(0xFFF6F3EE)
private val Muted = Color(0xFFB7A8A4)
private val Primary = Color(0xFFC8102E)
private val OnPrimary = Color(0xFFFFFFFF)
private val Danger = Color(0xFFC8102E)
private val Ok = Color(0xFF7D9A78)
private val Warn = Color(0xFFC4A15A)
private val FlagWhite = Color(0xFFF7F5F2)

sealed class Screen {
    data object Login : Screen()
    data object Register : Screen()
    data object Feed : Screen()
    data object NewListing : Screen()
    data class Listing(val id: String) : Screen()
    data object Map : Screen()
    data object Help : Screen()
    data object Messages : Screen()
    data class Thread(val id: String) : Screen()
    data object People : Screen()
    data class Person(val id: String) : Screen()
    data object Profile : Screen()
    data object Mesh : Screen()
    data object Status : Screen()
    data object Crisis : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = SiatkaApp.instance.db
        setContent { SiatkaRoot(db, this) }
    }
}

@Composable
private fun SiatkaRoot(db: SiatkaDb, activity: MainActivity) {
    val colors = darkColorScheme(
        primary = Primary, onPrimary = OnPrimary, background = Bg, surface = SurfaceC,
        onBackground = Fg, onSurface = Fg, error = Danger,
    )
    var tick by remember { mutableIntStateOf(0) }
    fun refresh() { tick++ }
    val session = remember(tick) { db.sessionId()?.let { db.profile(it) } }
    val stack = remember { mutableStateListOf<Screen>(if (session == null) Screen.Login else Screen.Feed) }
    var simulateOffline by remember { mutableStateOf(false) }
    fun go(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replace(s: Screen) { stack.clear(); stack.add(s) }
    val current = stack.last()
    BackHandler(enabled = stack.size > 1) { pop() }

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (session == null || current is Screen.Login || current is Screen.Register) {
                AuthScreen(db, current is Screen.Register, {
                    replace(Screen.Feed); refresh()
                }, { replace(if (current is Screen.Register) Screen.Login else Screen.Register) })
            } else if (current is Screen.Crisis) {
                CrisisScreen(db, session, activity, { pop(); refresh() })
            } else {
                Scaffold(
                    containerColor = Bg,
                    bottomBar = {
                        Row(
                            Modifier.fillMaxWidth().background(SurfaceC).padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            NavItem("Ogłoszenia", Icons.Outlined.GridView, current is Screen.Feed) { replace(Screen.Feed) }
                            NavItem("Mapa", Icons.Outlined.Map, current is Screen.Map) { replace(Screen.Map) }
                            NavItem("Pomoc", Icons.Outlined.FavoriteBorder, current is Screen.Help) { replace(Screen.Help) }
                            NavItem("Czat", Icons.Outlined.ChatBubbleOutline, current is Screen.Messages || current is Screen.Thread) { replace(Screen.Messages) }
                            NavItem("Profil", Icons.Outlined.Person, current is Screen.Profile) { replace(Screen.Profile) }
                        }
                    },
                ) { pad ->
                    Column(Modifier.padding(pad).fillMaxSize()) {
                        FlagStripe()
                        ConnectionStrip(activity, simulateOffline)
                        Box(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp)) {
                            when (current) {
                                Screen.Feed -> FeedScreen(db, { go(Screen.NewListing) }, { go(Screen.Listing(it)) })
                                Screen.NewListing -> NewListingScreen(db, session) { pop(); refresh() }
                                is Screen.Listing -> ListingScreen(db, session, current.id, { go(Screen.Thread(it)) }, { go(Screen.Person(it)) })
                                Screen.Map -> MapScreen(db, activity)
                                Screen.Help -> HelpScreen(db, session) { go(Screen.Thread(it)) }
                                Screen.Messages -> MessagesScreen(db, session) { go(Screen.Thread(it)) }
                                is Screen.Thread -> ThreadScreen(db, session, current.id) { refresh() }
                                Screen.People -> PeopleScreen(db) { go(Screen.Person(it)) }
                                is Screen.Person -> PersonScreen(db, session, current.id) { go(Screen.Thread(it)) }
                                Screen.Profile -> ProfileScreen(db, session, {
                                    db.logout(); replace(Screen.Login); refresh()
                                }, { go(it) })
                                Screen.Mesh -> MeshScreen(db) { refresh() }
                                Screen.Status -> StatusScreen(db, activity, simulateOffline, { simulateOffline = it }, { refresh() })
                                else -> {}
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    Button(
                        onClick = { go(Screen.Crisis) },
                        modifier = Modifier.padding(end = 16.dp, bottom = 96.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                    ) { Text("KRYZYS", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun FlagStripe() {
    Column(Modifier.fillMaxWidth().height(6.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().background(FlagWhite))
        Box(Modifier.weight(1f).fillMaxWidth().background(Primary))
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
                        Icon(icon, label, tint = if (selected) Primary else Muted, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = if (selected) Primary else Muted)
    }
}

@Composable
private fun ConnectionStrip(activity: MainActivity, simulate: Boolean) {
    var loraTick by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val cb: () -> Unit = { loraTick += 1 }
        LoraRadio.onChange(cb)
        onDispose { LoraRadio.removeChange(cb) }
    }
    val online = Comm.online(activity) && !simulate
    val lora = LoraRadio.phase == LoraRadio.Phase.Linked
    val label = when {
        lora && online -> "IP + LoRa EU868 · ${LoraRadio.deviceName ?: "Heltec V4"}"
        lora -> "LoRa EU868 · bez IP · ${LoraRadio.deviceName ?: "Heltec V4"}"
        online -> "Sieć IP · InternetAdapter aktywny"
        else -> "Tryb offline · kolejka store-and-forward"
    }
    Row(
        Modifier.fillMaxWidth().background(if (online || lora) SurfaceC else Elevated).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (lora) Icons.Outlined.Sensors else if (online) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
            null,
            tint = if (lora || online) Ok else Warn,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (online || lora) Muted else Warn, fontSize = 11.sp)
        if (loraTick < 0) Text("")
    }
}

@Composable
private fun AuthScreen(db: SiatkaDb, register: Boolean, onIn: () -> Unit, toggle: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        FlagStripe()
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
        Image(
            painter = painterResource(R.drawable.wataha_icon),
            contentDescription = "Polska Wataha",
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(16.dp))
        Text("POLSKA WATAHA", color = Primary, letterSpacing = 4.sp, fontSize = 12.sp)
        Text("Polska Wataha", color = Fg, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Text("Zaloguj się własnym kontem. Hasła są hashowane (PBKDF2), po 5 błędach blokada 5 min.", color = Muted, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        if (register) Field("Imię", name) { name = it }
        Field("Email", email) { email = it }
        Field("Hasło", pass, true) { pass = it }
        err?.let { Text(it, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        PrimaryBtn(if (register) "Utwórz konto" else "Zaloguj") {
            err = null
            try {
                if (register) {
                    if (name.isBlank() || email.isBlank() || pass.length < 8) err = "Uzupełnij dane (hasło min. 8)"
                    else { db.register(email, pass, name); onIn() }
                } else {
                    if (db.login(email, pass) == null) err = "Błędny email lub hasło" else onIn()
                }
            } catch (e: SiatkaDb.AuthException) { err = e.message }
            catch (e: Exception) { err = e.message }
        }
        Spacer(Modifier.height(8.dp))
        SecondaryBtn("Konto testowe (offline)") {
            try {
                db.login("demo@siatka.app", "siatka-demo-2026")
                onIn()
            } catch (e: Exception) { err = e.message }
        }
        Text(if (register) "Masz konto? Zaloguj" else "Utwórz własne konto", color = Muted, modifier = Modifier.padding(top = 16.dp).clickable { toggle() })
        }
    }
}

@Composable
private fun FeedScreen(db: SiatkaDb, onNew: () -> Unit, onOpen: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("all") }
    val items = db.listings(q, kind)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Label("Tablica"); Title("Ogłoszenia") }
            PrimaryBtn("Nowe", small = true, onClick = onNew)
        }
        OutlinedField(q, "Szukaj: wiertarka, Mokotów…") { q = it }
        LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("all" to "Wszystkie", "offer" to "Oferuję", "want" to "Szukam", "giveaway" to "Oddam", "exchange" to "Wymiana")) { (k, l) ->
                FilterChip(k == kind, l) { kind = k }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                CardBox({ onOpen(item.id) }) {
                    Text(kindLabel(item.kind), color = Muted, fontSize = 11.sp)
                    Text(item.title, color = Fg, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(item.body, color = Muted, fontSize = 13.sp, maxLines = 2)
                    Text("${item.authorName} · ${item.district}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun NewListingScreen(db: SiatkaDb, me: Profile, onDone: () -> Unit) {
    var kind by remember { mutableStateOf("offer") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Narzędzia") }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label("Nowe"); Title("Ogłoszenie")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("offer", "want", "giveaway", "exchange").forEach { k ->
                FilterChip(kind == k, kindLabel(k)) { kind = k }
            }
        }
        OutlinedField(title, "Tytuł") { title = it }
        OutlinedField(body, "Opis") { body = it }
        OutlinedField(cat, "Kategoria") { cat = it }
        PrimaryBtn("Opublikuj") {
            if (title.length >= 3) {
                db.addListing(me.id, kind, title, body, cat, me.district)
                if (kind == "giveaway") db.addBadge(me.id, "Dawca", "Oddano rzecz")
                onDone()
            }
        }
    }
}

@Composable
private fun ListingScreen(db: SiatkaDb, me: Profile, id: String, onThread: (String) -> Unit, onPerson: (String) -> Unit) {
    val item = db.listing(id) ?: return Title("Brak ogłoszenia")
    var msg by remember { mutableStateOf("Cześć, jestem zainteresowany/a.") }
    var offer by remember { mutableStateOf("Mogę dać w zamian narzędzia / przysługę.") }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(kindLabel(item.kind), color = Muted, fontSize = 12.sp)
        Title(item.title)
        Text(item.body, color = Muted)
        CardBox({ onPerson(item.authorId) }) {
            Text(item.authorName, color = Fg, fontWeight = FontWeight.Medium)
            Text("${item.district} · ${item.category}", color = Muted, fontSize = 12.sp)
        }
        OutlinedField(msg, "Wiadomość") { msg = it }
        PrimaryBtn("Wyślij wiadomość") { onThread(db.send(me.id, item.authorId, msg)) }
        OutlinedField(offer, "Propozycja wymiany") { offer = it }
        SecondaryBtn("Zaproponuj wymianę") {
            db.addBadge(me.id, "Wymieniacz", "Zaproponowano wymianę")
            onThread(db.send(me.id, item.authorId, "Propozycja wymiany: $offer"))
        }
    }
}

@Composable
private fun MapScreen(db: SiatkaDb, activity: MainActivity) {
    var me by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var filter by remember { mutableStateOf("all") }
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) me = lastLocation(activity)
    }
    val pins = db.pins().filter { filter == "all" || it.tone == filter }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Label("Mapa watahy"); Title("Warszawa") }
            SecondaryBtn("Lokalizacja", small = true) {
                locPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
        Text("Mapa działa bez klucza Google. Piny z bazy lokalnej.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            items(listOf("all", "listing", "help", "need", "point", "node", "crisis")) { f ->
                FilterChip(filter == f, f) { filter = f }
            }
        }
        Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(16.dp)).background(Elevated).border(1.dp, Fg.copy(0.12f), RoundedCornerShape(16.dp))) {
            Text("WARSZAWA · WATAHA", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(12.dp))
            pins.forEach { pin ->
                val x = ((pin.lng - 20.84) / (21.2 - 20.84)).coerceIn(0.04, 0.96)
                val y = ((52.33 - pin.lat) / (52.33 - 52.14)).coerceIn(0.04, 0.96)
                Box(
                    Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart).offset(
                        x = (x * 320).roundToInt().dp, y = (y * 320).roundToInt().dp,
                    ).size(10.dp).clip(CircleShape).background(toneColor(pin.tone)),
                )
            }
            me?.let { (lat, lng) ->
                val x = ((lng - 20.84) / (21.2 - 20.84)).coerceIn(0.04, 0.96)
                val y = ((52.33 - lat) / (52.33 - 52.14)).coerceIn(0.04, 0.96)
                Box(
                    Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart).offset(
                        x = (x * 320).roundToInt().dp, y = (y * 320).roundToInt().dp,
                    ).size(12.dp).clip(CircleShape).background(Danger),
                )
            }
        }
        LazyColumn(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(pins.take(12)) { p ->
                Text("${p.tone} · ${p.label}", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HelpScreen(db: SiatkaDb, me: Profile, onThread: (String) -> Unit) {
    var kind by remember { mutableStateOf("need") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var urg by remember { mutableStateOf("normal") }
    var rev by remember { mutableIntStateOf(0) }
    val items = remember(rev) { db.help() }
    Column {
        Label("Wzajemność"); Title("Pomoc")
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(kind == "need", "Zgłoś potrzebę") { kind = "need" }
            FilterChip(kind == "offer", "Zaoferuj pomoc") { kind = "offer" }
        }
        OutlinedField(title, "Tytuł") { title = it }
        OutlinedField(body, "Szczegóły") { body = it }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            listOf("low", "normal", "high", "crisis").forEach { u -> FilterChip(urg == u, u) { urg = u } }
        }
        PrimaryBtn("Opublikuj") {
            if (title.length >= 3) {
                db.addHelp(me.id, kind, title, body, urg); title = ""; body = ""; rev++
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { h ->
                CardBox({}) {
                    Text("${if (h.kind == "need") "Potrzebuję" else "Mogę pomóc"} · ${h.urgency}", color = if (h.urgency == "high" || h.urgency == "crisis") Danger else Muted, fontSize = 11.sp)
                    Text(h.title, color = Fg, fontWeight = FontWeight.SemiBold)
                    Text(h.body, color = Muted, fontSize = 13.sp)
                    Text("${h.authorName} · ${h.district}", color = Muted, fontSize = 11.sp)
                    Text("Napisz", color = Primary, modifier = Modifier.padding(top = 6.dp).clickable {
                        onThread(db.send(me.id, h.authorId, if (h.kind == "need") "Mogę pomóc." else "Chętnie skorzystam."))
                    })
                }
            }
        }
    }
}

@Composable
private fun MessagesScreen(db: SiatkaDb, me: Profile, onOpen: (String) -> Unit) {
    val items = db.threads(me.id)
    Column {
        Label("Skrzynka"); Title("Wiadomości")
        if (items.isEmpty()) Text("Brak rozmów.", color = Muted, modifier = Modifier.padding(top = 12.dp))
        LazyColumn(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { t ->
                CardBox({ onOpen(t.id) }) {
                    Text(t.peerName, color = Fg, fontWeight = FontWeight.Medium)
                    Text(t.last, color = Muted, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ThreadScreen(db: SiatkaDb, me: Profile, id: String, onChange: () -> Unit) {
    var body by remember { mutableStateOf("") }
    var rev by remember { mutableIntStateOf(0) }
    val msgs = remember(rev) { db.messages(id) }
    Column {
        Title("Rozmowa")
        LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { m ->
                val mine = m.senderId == me.id
                Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(12.dp)).background(if (mine) Primary else Elevated).padding(10.dp)) {
                        Text(m.senderName, color = if (mine) Bg.copy(0.7f) else Muted, fontSize = 10.sp)
                        Text(m.body, color = if (mine) Bg else Fg, fontSize = 14.sp)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { OutlinedField(body, "Napisz…") { body = it } }
            Spacer(Modifier.width(8.dp))
            PrimaryBtn("Wyślij", small = true) {
                if (body.isNotBlank()) {
                    val peer = db.threads(me.id).find { it.id == id }?.peerId ?: me.id
                    db.send(me.id, peer, body, id); body = ""; rev++; onChange()
                    notify(SiatkaApp.instance, "messages", "Wiadomość", "Wysłano w Wataże")
                }
            }
        }
    }
}

@Composable
private fun PeopleScreen(db: SiatkaDb, onOpen: (String) -> Unit) {
    Column {
        Label("Sąsiedzi"); Title("Ludzie")
        LazyColumn(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(db.people(), key = { it.id }) { p ->
                CardBox({ onOpen(p.id) }) {
                    Text(p.name, color = Fg, fontWeight = FontWeight.SemiBold)
                    Text("${p.district} · reputacja ${p.reputation}", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PersonScreen(db: SiatkaDb, me: Profile, id: String, onThread: (String) -> Unit) {
    val p = db.profile(id) ?: return
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Title(p.name)
        Text("${p.district} · reputacja ${p.reputation}", color = Muted)
        Text(p.bio.ifBlank { "Brak opisu." }, color = Muted)
        db.badges(p.id).forEach { Text("· ${it.title}", color = Fg, fontSize = 13.sp) }
        PrimaryBtn("Wyślij wiadomość") { onThread(db.send(me.id, p.id, "Cześć! Piszę z Siatki.")) }
    }
}

@Composable
private fun ProfileScreen(db: SiatkaDb, me: Profile, onOut: () -> Unit, go: (Screen) -> Unit) {
    val fresh = db.profile(me.id) ?: me
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Title(fresh.name)
        Text("${fresh.district} · reputacja ${fresh.reputation}", color = Muted)
        Text(fresh.bio, color = Muted)
        Text("Odznaki", color = Fg, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        db.badges(fresh.id).forEach { Text("· ${it.title} — ${it.description}", color = Muted, fontSize = 13.sp) }
        SecondaryBtn("Sąsiedzi") { go(Screen.People) }
        SecondaryBtn("Radio LoRa · Heltec V4") { go(Screen.Mesh) }
        SecondaryBtn("Status i offline") { go(Screen.Status) }
        TextButton(onClick = onOut) { Text("Wyloguj", color = Danger) }
    }
}

@Composable
private fun MeshScreen(db: SiatkaDb, onChange: () -> Unit) {
    var from by remember { mutableStateOf("NODE_A") }
    var to by remember { mutableStateOf("NODE_D") }
    var rev by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var tx by remember { mutableStateOf("WATAHA ping") }
    var radioTick by remember { mutableIntStateOf(0) }
    val activity = SiatkaApp.instance
    val btPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ok ->
        if (ok.values.all { it }) LoraRadio.startScan(activity)
    }
    DisposableEffect(Unit) {
        val cb: () -> Unit = { radioTick += 1; onChange() }
        LoraRadio.onChange(cb)
        onDispose { LoraRadio.removeChange(cb) }
    }
    val nodes = remember(rev) { db.nodes() }
    val hop = Comm.route(nodes, from, to)
    val found = LoraRadio.found.toList()
    val log = LoraRadio.log.toList()
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label("V0.4 · SX1262"); Title("Heltec LoRa 32 V4")
        Text(
            "WiFi LoRa 32 V4 · pasmo HF 863–928 MHz (EU868). Wgraj firmware Watahy, na OLED pojawi się PIN. Skanuj BLE i sparuj.",
            color = Muted, fontSize = 13.sp,
        )
        CardBox({}) {
            Text(LoraRadio.phase.name, color = if (LoraRadio.phase == LoraRadio.Phase.Linked) Ok else Fg, fontWeight = FontWeight.SemiBold)
            Text(LoraRadio.detail, color = Muted, fontSize = 13.sp)
            LoraRadio.lastRssi?.let { Text("RSSI $it dBm", color = Muted, fontSize = 12.sp) }
        }
        PrimaryBtn("Skanuj bramkę BLE") {
            val perms = if (Build.VERSION.SDK_INT >= 31)
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            else arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
            btPerm.launch(perms)
        }
        found.forEach { d ->
            CardBox({
                if (pin.length == 6) {
                    LoraRadio.connect(activity, d.address, pin)
                }
            }) {
                Text(d.name, color = Fg, fontWeight = FontWeight.SemiBold)
                Text("${d.address} · ${d.rssi} dBm", color = Muted, fontSize = 12.sp)
                Text(if (pin.length == 6) "Dotknij, żeby wysłać PIN do weryfikacji" else "Najpierw wpisz 6-cyfrowy PIN z OLED", color = Primary, fontSize = 12.sp)
            }
        }
        OutlinedField(pin, "PIN z wyświetlacza OLED") { pin = it.filter { ch -> ch.isDigit() }.take(6) }
        if (LoraRadio.phase == LoraRadio.Phase.Linked) {
            LaunchedEffect(LoraRadio.deviceAddress) {
                val addr = LoraRadio.deviceAddress ?: return@LaunchedEffect
                db.rememberLora(addr, LoraRadio.deviceName ?: "Heltec V4")
            }
            OutlinedField(tx, "Ramka LoRa") { tx = it }
            PrimaryBtn("Wyślij przez SX1262") {
                val ok = LoraRadio.sendText(tx)
                if (!ok) db.enqueue("lora.hold", tx)
                radioTick++
            }
            SecondaryBtn("Rozłącz") { LoraRadio.disconnect(); radioTick++ }
        }
        Title("Dziennik radia")
        if (log.isEmpty()) Text("Brak ramek. Po sparowaniu zobaczysz RX/TX.", color = Muted, fontSize = 13.sp)
        log.take(12).forEach { Text(it, color = Muted, fontSize = 11.sp) }

        Label("Mesh"); Title("Symulator węzłów")
        Text("A → B → C → D. NODE D to Heltec V4. Wyłącz B, żeby zobaczyć rerouting.", color = Muted, fontSize = 13.sp)
        nodes.forEach { n ->
            CardBox({}) {
                Text(n.id.replace("_", " "), color = Fg, fontWeight = FontWeight.SemiBold)
                Text("${n.label} · ${n.kind} · ${n.status}", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf("online", "degraded", "down").forEach { st ->
                        FilterChip(n.status == st, st) { db.setNode(n.id, st); rev++; onChange() }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("NODE_A", "NODE_B", "NODE_C", "NODE_D").forEach { id ->
                FilterChip(from == id, id.takeLast(1)) { from = id }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("NODE_A", "NODE_B", "NODE_C", "NODE_D").forEach { id ->
                FilterChip(to == id, "→${id.takeLast(1)}") { to = id }
            }
        }
        CardBox({}) {
            Text(if (hop.ok) "Dostarczono" else "Brak trasy", color = if (hop.ok) Ok else Danger, fontWeight = FontWeight.SemiBold)
            Text("$from → ${hop.via.joinToString(" → ").ifBlank { "—" }} → $to", color = Fg, fontSize = 13.sp)
            hop.reason?.let { Text(it, color = Warn, fontSize = 12.sp) }
        }
        SecondaryBtn("Zasymuluj awarię NODE B") { db.setNode("NODE_B", "down"); rev++; onChange() }
        if (radioTick < 0) Text("")
    }
}

@Composable
private fun StatusScreen(db: SiatkaDb, activity: MainActivity, simulate: Boolean, setSim: (Boolean) -> Unit, onChange: () -> Unit) {
    val btPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val adapters = Comm.adapters(activity, simulate)
    val q = db.queue()
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label("Warstwa komunikacji"); Title("Status sieci")
        Text("Aplikacja → Communication Service → Adapter → Internet / BT / Wi-Fi Direct / LoRa", color = Muted, fontSize = 13.sp)
        SecondaryBtn(if (simulate) "Włącz sieć" else "Testuj offline") { setSim(!simulate) }
        adapters.forEach { a ->
            CardBox({}) {
                Text(a.label, color = Fg, fontWeight = FontWeight.Medium)
                Text("${a.layer} · ${a.health}", color = Muted, fontSize = 12.sp)
                Text(a.detail, color = Muted, fontSize = 13.sp)
                Text(a.stage, color = Muted, fontSize = 11.sp)
            }
        }
        SecondaryBtn("Skanuj Heltec V4 (Bluetooth)") {
            val perms = if (Build.VERSION.SDK_INT >= 31)
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            else arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
            btPerm.launch(perms)
            LoraRadio.startScan(activity)
        }
        Title("Kolejka")
        Text("${q.count { !it.synced }} oczekujących, ${q.count { it.synced }} zsynchronizowanych", color = Muted, fontSize = 13.sp)
        PrimaryBtn("Synchronizuj") { db.flushQueue(); onChange() }
        q.take(12).forEach { Text("${if (it.synced) "synced" else "queued"} · ${it.action}", color = Muted, fontSize = 12.sp) }
    }
}

@Composable
private fun CrisisScreen(db: SiatkaDb, me: Profile, activity: MainActivity, onClose: () -> Unit) {
    var note by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf<String?>(null) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ok ->
        val loc = if (ok.values.any { it }) lastLocation(activity) else null
        val body = note.ifBlank { "Lokalizacja ${loc?.first}, ${loc?.second}" }
        db.addCrisis(me.id, "location", body, loc?.first, loc?.second)
        val radio = LoraRadio.sendCrisis("location", body)
        if (!radio) db.enqueue("crisis.location", body)
        notify(activity, "crisis", "Kryzys", if (radio) "Lokalizacja przez LoRa" else "Wysłano lokalizację")
        sent = if (radio) "Lokalizacja · LoRa" else "Lokalizacja · kolejka"
    }
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        FlagStripe()
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TRYB KRYZYSOWY", color = Muted, letterSpacing = 3.sp, fontSize = 12.sp)
            Text("Zamknij", color = Fg, modifier = Modifier.clickable { onClose() })
        }
        Title("Polska Wataha")
        Text("Czarne Wilki Prawdy · duże przyciski, bez zbędnych animacji.", color = Muted)
        OutlinedField(note, "Krótki komunikat") { note = it }
        CrisisBtn("Potrzebuję pomocy") {
            askNotif(activity, notifPerm)
            val loc = lastLocation(activity)
            val body = note.ifBlank { "Potrzebuję pomocy" }
            db.addCrisis(me.id, "need_help", body, loc?.first, loc?.second)
            val radio = LoraRadio.sendCrisis("need_help", body)
            if (!radio) db.enqueue("crisis.need_help", body)
            notify(activity, "crisis", "Kryzys", if (radio) "Wysłano przez LoRa" else "W kolejce — brak radia")
            sent = if (radio) "Potrzebuję pomocy · LoRa" else "Potrzebuję pomocy · kolejka"
        }
        CrisisBtn("Mogę pomóc") {
            val body = note.ifBlank { "Mogę pomóc" }
            db.addCrisis(me.id, "can_help", body, null, null)
            val radio = LoraRadio.sendCrisis("can_help", body)
            if (!radio) db.enqueue("crisis.can_help", body)
            notify(activity, "help", "Pomoc", if (radio) "Zadeklarowano przez LoRa" else "W kolejce")
            sent = if (radio) "Mogę pomóc · LoRa" else "Mogę pomóc · kolejka"
        }
        CrisisBtn("Komunikat") {
            val body = note.ifBlank { "Komunikat sieci Polska Wataha" }
            db.addCrisis(me.id, "broadcast", body, null, null)
            val radio = LoraRadio.sendCrisis("broadcast", body)
            if (!radio) db.enqueue("crisis.broadcast", body)
            notify(activity, "crisis", "Komunikat", if (radio) "Nadano LoRa" else body)
            sent = if (radio) "Komunikat · LoRa" else "Komunikat · kolejka"
        }
        CrisisBtn("Moja lokalizacja") {
            locPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        SecondaryBtn("Mapa") { onClose() }
        SecondaryBtn("Status sieci") { onClose() }
        sent?.let { Text("Wysłano: $it", color = Ok) }
        }
    }
}

@Composable private fun Label(t: String) = Text(t.uppercase(), color = Muted, letterSpacing = 2.sp, fontSize = 11.sp)
@Composable private fun Title(t: String) = Text(t, color = Fg, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)

@Composable
private fun CardBox(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceC).border(1.dp, Fg.copy(0.12f), RoundedCornerShape(16.dp)).clickable { onClick() }.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp), content = content,
    )
}

@Composable
private fun FilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) Primary else Elevated).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(label, color = if (selected) OnPrimary else Fg, fontSize = 12.sp) }
}

@Composable
private fun Field(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    OutlinedTextField(
        value, onChange, modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = fieldColors(),
    )
}

@Composable
private fun OutlinedField(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), placeholder = { Text(placeholder, color = Muted) }, colors = fieldColors())
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Fg, unfocusedTextColor = Fg, focusedBorderColor = Primary, unfocusedBorderColor = Fg.copy(0.2f),
    focusedContainerColor = Elevated, unfocusedContainerColor = Elevated, cursorColor = Primary,
)

@Composable
private fun PrimaryBtn(label: String, small: Boolean = false, onClick: () -> Unit) {
    Button(onClick, modifier = Modifier.then(if (small) Modifier else Modifier.fillMaxWidth().height(48.dp)), colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary), shape = RoundedCornerShape(10.dp)) {
        Text(label, fontWeight = FontWeight.Medium)
    }
}
@Composable
private fun SecondaryBtn(label: String, small: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(onClick, modifier = Modifier.then(if (small) Modifier else Modifier.fillMaxWidth().height(48.dp)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Fg), shape = RoundedCornerShape(10.dp)) { Text(label) }
}
@Composable
private fun CrisisBtn(label: String, onClick: () -> Unit) {
    Button(onClick, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary), shape = RoundedCornerShape(12.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

private fun kindLabel(k: String) = when (k) {
    "offer" -> "Oferuję"; "want" -> "Szukam"; "giveaway" -> "Oddam"; "exchange" -> "Wymiana"; else -> k
}
private fun toneColor(t: String) = when (t) {
    "listing" -> Primary; "help" -> Ok; "need" -> Warn; "node" -> Fg; "crisis" -> Danger; else -> Muted
}

private fun lastLocation(activity: MainActivity): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    ) return null
    return try {
        val lm = activity.getSystemService(LocationManager::class.java)
        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        loc?.let { it.latitude to it.longitude }
    } catch (_: SecurityException) { null }
}

private fun askNotif(activity: MainActivity, launcher: androidx.activity.result.ActivityResultLauncher<String>) {
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun notify(ctx: android.content.Context, channel: String, title: String, body: String) {
    val nm = ctx.getSystemService(NotificationManager::class.java)
    val n = NotificationCompat.Builder(ctx, channel)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(if (channel == "crisis") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        .build()
    nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
}
