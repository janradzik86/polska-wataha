package app.siatka

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Profile(
    val id: String, val name: String, val bio: String, val district: String,
    val lat: Double, val lng: Double, val reputation: Int, val hue: Int, val password: String? = null,
)
data class Listing(
    val id: String, val authorId: String, val authorName: String, val hue: Int,
    val kind: String, val title: String, val body: String, val category: String,
    val district: String, val lat: Double, val lng: Double, val createdAt: Long,
)
data class HelpPost(
    val id: String, val authorId: String, val authorName: String, val hue: Int,
    val kind: String, val title: String, val body: String, val urgency: String,
    val district: String, val lat: Double, val lng: Double,
)
data class Chat(
    val id: String, val peerId: String, val peerName: String, val last: String, val at: Long,
)
data class Msg(val id: String, val threadId: String, val senderId: String, val senderName: String, val body: String, val at: Long)
data class Node(val id: String, val label: String, val kind: String, val lat: Double, val lng: Double, var status: String)
data class Pin(val id: String, val lat: Double, val lng: Double, val label: String, val tone: String)
data class Badge(val title: String, val description: String)
data class QueueItem(val id: String, val action: String, val payload: String, val synced: Boolean)

class SiatkaDb(ctx: Context) : SQLiteOpenHelper(ctx, "siatka.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table users(id text primary key, email text unique, name text, password text, district text, bio text, lat real, lng real, reputation int, hue int)")
        db.execSQL("create table listings(id text primary key, author_id text, kind text, title text, body text, category text, district text, lat real, lng real, created_at int)")
        db.execSQL("create table help(id text primary key, author_id text, kind text, title text, body text, urgency text, district text, lat real, lng real)")
        db.execSQL("create table threads(id text primary key, a text, b text)")
        db.execSQL("create table messages(id text primary key, thread_id text, sender_id text, body text, created_at int)")
        db.execSQL("create table badges(profile_id text, title text, description text)")
        db.execSQL("create table nodes(id text primary key, label text, kind text, lat real, lng real, status text)")
        db.execSQL("create table points(id text primary key, title text, kind text, lat real, lng real)")
        db.execSQL("create table crisis(id text primary key, author_id text, kind text, body text, lat real, lng real, created_at int)")
        db.execSQL("create table queue(id text primary key, action text, payload text, created_at int, synced int default 0)")
        db.execSQL("create table session(k text primary key, v text)")
    }
    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}

    fun seedIfEmpty() {
        val db = writableDatabase
        val c = db.rawQuery("select count(*) from users", null)
        c.moveToFirst()
        val n = c.getInt(0)
        c.close()
        if (n > 0) return
        fun user(id: String, email: String, name: String, pass: String?, dist: String, bio: String, lat: Double, lng: Double, rep: Int, hue: Int) {
            db.insert("users", null, ContentValues().apply {
                put("id", id); put("email", email); put("name", name); put("password", pass)
                put("district", dist); put("bio", bio); put("lat", lat); put("lng", lng)
                put("reputation", rep); put("hue", hue)
            })
        }
        user("me", "demo@siatka.app", "Anna Kowalska", "siatka-demo-2026", "Mokotów", "Konto demonstracyjne Polskiej Watahy.", 52.22, 21.01, 18, 48)
        user("p_marek", "marek@siatka.app", "Marek Nowak", null, "Wola", "Stolarz. Pożyczam narzędzia.", 52.231, 20.984, 42, 28)
        user("p_ewa", "ewa@siatka.app", "Ewa Wiśniewska", null, "Mokotów", "Pielęgniarka, dyżury nocne.", 52.201, 21.017, 61, 200)
        user("p_piotr", "piotr@siatka.app", "Piotr Zieliński", null, "Praga-Północ", "Rowerzysta, kurier.", 52.256, 21.035, 33, 140)
        user("p_lena", "lena@siatka.app", "Lena Kruk", null, "Żoliborz", "Ogrodniczka, sadzonki.", 52.269, 20.986, 54, 90)
        user("p_jan", "jan@siatka.app", "Jan Olszewski", null, "Ochota", "Emerytowany elektryk.", 52.214, 20.981, 47, 320)

        fun listing(id: String, a: String, kind: String, title: String, body: String, cat: String, dist: String, lat: Double, lng: Double) {
            db.insert("listings", null, ContentValues().apply {
                put("id", id); put("author_id", a); put("kind", kind); put("title", title); put("body", body)
                put("category", cat); put("district", dist); put("lat", lat); put("lng", lng); put("created_at", System.currentTimeMillis())
            })
        }
        listing("l1", "p_marek", "offer", "Wiertarka udarowa na weekend", "Bosch 800W, walizka, wiertła do betonu.", "Narzędzia", "Wola", 52.231, 20.984)
        listing("l2", "p_lena", "giveaway", "Sadzonki pomidorów Malinowy Ożarski", "Dwanaście sztuk, zahartowane.", "Ogród", "Żoliborz", 52.269, 20.986)
        listing("l3", "p_ewa", "want", "Szukam wózka dziecięcego", "Na kilka miesięcy. Stan używany OK.", "Dzieci", "Mokotów", 52.201, 21.017)
        listing("l4", "p_piotr", "exchange", "Rower miejski za hulajnogę", "Kross Trans 5.0, 28 cali.", "Transport", "Praga-Północ", 52.256, 21.035)
        listing("l5", "p_jan", "offer", "Sprawdzenie instalacji w mieszkaniu", "Bezpieczniki, gniazdka, oświetlenie.", "Dom", "Ochota", 52.214, 20.981)
        listing("l6", "p_lena", "giveaway", "Słoiki 0,9 l — 24 sztuki", "Wykiprowane, z zakrętkami.", "Dom", "Żoliborz", 52.268, 20.988)
        listing("l7", "p_marek", "offer", "Przyczepka rowerowa", "Na dwa rowery albo zakupy.", "Transport", "Wola", 52.233, 20.987)
        listing("l8", "p_ewa", "giveaway", "Odzież dziecięca 98–104", "Kurtka, spodnie, swetry.", "Odzież", "Mokotów", 52.203, 21.02)

        fun help(id: String, a: String, kind: String, title: String, body: String, urg: String, dist: String, lat: Double, lng: Double) {
            db.insert("help", null, ContentValues().apply {
                put("id", id); put("author_id", a); put("kind", kind); put("title", title); put("body", body)
                put("urgency", urg); put("district", dist); put("lat", lat); put("lng", lng)
            })
        }
        help("h1", "p_jan", "need", "Zakupy dla osoby starszej", "Chleb, mleko, leki z apteki na Banacha.", "high", "Ochota", 52.214, 20.981)
        help("h2", "p_ewa", "offer", "Opieka po zabiegu — dyżur wieczorny", "3–4 godziny, ciśnienie, leki.", "normal", "Mokotów", 52.201, 21.017)
        help("h3", "p_piotr", "offer", "Transport na SOR / przychodnię", "Auto, Praga i Śródmieście.", "high", "Praga-Północ", 52.256, 21.035)
        help("h4", "p_lena", "need", "Podlewanie ogrodu 12–18 maja", "Dwa grządki i donice.", "low", "Żoliborz", 52.269, 20.986)
        help("h5", "p_marek", "offer", "Wniesienie mebli", "Weekend, dwie pary rąk.", "normal", "Wola", 52.231, 20.984)

        db.insert("threads", null, ContentValues().apply { put("id", "th1"); put("a", "me"); put("b", "p_marek") })
        db.insert("messages", null, ContentValues().apply {
            put("id", "m1"); put("thread_id", "th1"); put("sender_id", "p_marek")
            put("body", "Cześć! Wiertarka wolna w sobotę od 10."); put("created_at", System.currentTimeMillis() - 3600_000)
        })
        db.insert("messages", null, ContentValues().apply {
            put("id", "m2"); put("thread_id", "th1"); put("sender_id", "me")
            put("body", "Dzięki, biorę. Wpadnę koło 10:30."); put("created_at", System.currentTimeMillis() - 1800_000)
        })

        fun badge(id: String, t: String, d: String) {
            db.insert("badges", null, ContentValues().apply { put("profile_id", id); put("title", t); put("description", d) })
        }
        badge("me", "Pierwszy post", "Konto demonstracyjne")
        badge("me", "Offline hero", "Działanie bez internetu")
        badge("p_marek", "Pomocnik", "Zaoferowano pomoc")
        badge("p_ewa", "Sąsiad roku", "Wysoka reputacja")
        badge("p_jan", "Strażnik sieci", "Tryb kryzysowy")
        badge("p_lena", "Dawca", "Oddano rzecz")
        badge("p_piotr", "Offline hero", "Praca bez IP")

        fun node(id: String, label: String, kind: String, lat: Double, lng: Double, st: String) {
            db.insert("nodes", null, ContentValues().apply {
                put("id", id); put("label", label); put("kind", kind); put("lat", lat); put("lng", lng); put("status", st)
            })
        }
        node("NODE_A", "NODE A — Śródmieście", "phone", 52.2297, 21.0122, "online")
        node("NODE_B", "NODE B — Wola", "wifi", 52.231, 20.984, "online")
        node("NODE_C", "NODE C — Praga", "bt", 52.256, 21.035, "online")
        node("NODE_D", "NODE D — Mokotów (LoRa)", "lora", 52.201, 21.017, "degraded")

        db.insert("points", null, ContentValues().apply { put("id", "hp1"); put("title", "Punkt wody — Plac Wilsona"); put("kind", "water"); put("lat", 52.269); put("lng", 20.986) })
        db.insert("points", null, ContentValues().apply { put("id", "hp2"); put("title", "Punkt medyczny"); put("kind", "medical"); put("lat", 52.226); put("lng", 21.012) })
        db.insert("points", null, ContentValues().apply { put("id", "hp3"); put("title", "Schronienie — Targowa"); put("kind", "shelter"); put("lat", 52.251); put("lng", 21.038) })
        db.insert("points", null, ContentValues().apply { put("id", "hp4"); put("title", "Punkt żywności — Hala Mirowska"); put("kind", "food"); put("lat", 52.239); put("lng", 20.998) })
    }

    fun login(email: String, password: String): Profile? {
        val c = readableDatabase.rawQuery("select * from users where email=? and password=?", arrayOf(email, password))
        val p = c.toProfile()
        c.close()
        if (p != null) setSession(p.id)
        return p
    }

    fun register(email: String, password: String, name: String): Profile {
        val id = "u_${System.currentTimeMillis()}"
        writableDatabase.insert("users", null, ContentValues().apply {
            put("id", id); put("email", email); put("name", name); put("password", password)
            put("district", "Śródmieście"); put("bio", ""); put("lat", 52.2297); put("lng", 21.0122)
            put("reputation", 0); put("hue", 175)
        })
        writableDatabase.insert("badges", null, ContentValues().apply {
            put("profile_id", id); put("title", "Pierwszy post"); put("description", "Konto utworzone")
        })
        setSession(id)
        return profile(id)!!
    }

    fun setSession(id: String) {
        writableDatabase.replace("session", null, ContentValues().apply { put("k", "uid"); put("v", id) })
    }
    fun sessionId(): String? {
        val c = readableDatabase.rawQuery("select v from session where k='uid'", null)
        val v = if (c.moveToFirst()) c.getString(0) else null
        c.close()
        return v
    }
    fun logout() { writableDatabase.delete("session", "k=?", arrayOf("uid")) }

    fun profile(id: String): Profile? {
        val c = readableDatabase.rawQuery("select * from users where id=?", arrayOf(id))
        val p = c.toProfile()
        c.close()
        return p
    }
    fun people(): List<Profile> {
        val c = readableDatabase.rawQuery("select * from users order by reputation desc", null)
        val out = mutableListOf<Profile>()
        while (c.moveToNext()) out += c.readProfile()
        c.close()
        return out
    }
    fun listings(q: String = "", kind: String = "all"): List<Listing> {
        val c = readableDatabase.rawQuery(
            "select l.*, u.name, u.hue from listings l join users u on u.id=l.author_id order by l.created_at desc",
            null,
        )
        val out = mutableListOf<Listing>()
        while (c.moveToNext()) {
            val item = Listing(
                c.getString(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("author_id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getInt(c.getColumnIndexOrThrow("hue")),
                c.getString(c.getColumnIndexOrThrow("kind")),
                c.getString(c.getColumnIndexOrThrow("title")),
                c.getString(c.getColumnIndexOrThrow("body")),
                c.getString(c.getColumnIndexOrThrow("category")),
                c.getString(c.getColumnIndexOrThrow("district")),
                c.getDouble(c.getColumnIndexOrThrow("lat")),
                c.getDouble(c.getColumnIndexOrThrow("lng")),
                c.getLong(c.getColumnIndexOrThrow("created_at")),
            )
            val matchQ = q.isBlank() || item.title.contains(q, true) || item.body.contains(q, true) || item.district.contains(q, true)
            val matchK = kind == "all" || item.kind == kind
            if (matchQ && matchK) out += item
        }
        c.close()
        return out
    }
    fun listing(id: String) = listings().find { it.id == id }
    fun addListing(author: String, kind: String, title: String, body: String, cat: String, dist: String) {
        val id = "lst_${System.currentTimeMillis()}"
        val p = profile(author)
        writableDatabase.insert("listings", null, ContentValues().apply {
            put("id", id); put("author_id", author); put("kind", kind); put("title", title); put("body", body)
            put("category", cat); put("district", dist); put("lat", p?.lat ?: 52.23); put("lng", p?.lng ?: 21.01)
            put("created_at", System.currentTimeMillis())
        })
        bump(author, 2)
        enqueue("listing.create", id)
    }
    fun help(): List<HelpPost> {
        val c = readableDatabase.rawQuery("select h.*, u.name, u.hue from help h join users u on u.id=h.author_id", null)
        val out = mutableListOf<HelpPost>()
        while (c.moveToNext()) {
            out += HelpPost(
                c.getString(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("author_id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getInt(c.getColumnIndexOrThrow("hue")),
                c.getString(c.getColumnIndexOrThrow("kind")),
                c.getString(c.getColumnIndexOrThrow("title")),
                c.getString(c.getColumnIndexOrThrow("body")),
                c.getString(c.getColumnIndexOrThrow("urgency")),
                c.getString(c.getColumnIndexOrThrow("district")),
                c.getDouble(c.getColumnIndexOrThrow("lat")),
                c.getDouble(c.getColumnIndexOrThrow("lng")),
            )
        }
        c.close()
        return out.sortedBy { if (it.urgency == "crisis") 0 else if (it.urgency == "high") 1 else 2 }
    }
    fun addHelp(author: String, kind: String, title: String, body: String, urg: String) {
        val p = profile(author)
        writableDatabase.insert("help", null, ContentValues().apply {
            put("id", "h_${System.currentTimeMillis()}"); put("author_id", author); put("kind", kind)
            put("title", title); put("body", body); put("urgency", urg); put("district", p?.district ?: "")
            put("lat", p?.lat ?: 52.23); put("lng", p?.lng ?: 21.01)
        })
        if (kind == "offer") bump(author, 3)
        enqueue("help.create", title)
    }
    fun threads(me: String): List<Chat> {
        val c = readableDatabase.rawQuery("select * from threads where a=? or b=?", arrayOf(me, me))
        val out = mutableListOf<Chat>()
        while (c.moveToNext()) {
            val id = c.getString(c.getColumnIndexOrThrow("id"))
            val a = c.getString(c.getColumnIndexOrThrow("a"))
            val b = c.getString(c.getColumnIndexOrThrow("b"))
            val peer = if (a == me) b else a
            val p = profile(peer)
            val last = lastMsg(id)
            out += Chat(id, peer, p?.name ?: peer, last?.body ?: "", last?.at ?: 0)
        }
        c.close()
        return out.sortedByDescending { it.at }
    }
    fun messages(threadId: String): List<Msg> {
        val c = readableDatabase.rawQuery("select * from messages where thread_id=? order by created_at", arrayOf(threadId))
        val out = mutableListOf<Msg>()
        while (c.moveToNext()) {
            val sid = c.getString(c.getColumnIndexOrThrow("sender_id"))
            out += Msg(
                c.getString(c.getColumnIndexOrThrow("id")),
                threadId, sid, profile(sid)?.name ?: sid,
                c.getString(c.getColumnIndexOrThrow("body")),
                c.getLong(c.getColumnIndexOrThrow("created_at")),
            )
        }
        c.close()
        return out
    }
    fun send(me: String, to: String, body: String, threadId: String? = null): String {
        var tid = threadId
        if (tid == null) {
            val c = readableDatabase.rawQuery("select id from threads where (a=? and b=?) or (a=? and b=?)", arrayOf(me, to, to, me))
            tid = if (c.moveToFirst()) c.getString(0) else null
            c.close()
            if (tid == null) {
                tid = "th_${System.currentTimeMillis()}"
                writableDatabase.insert("threads", null, ContentValues().apply { put("id", tid); put("a", me); put("b", to) })
            }
        }
        writableDatabase.insert("messages", null, ContentValues().apply {
            put("id", "msg_${System.currentTimeMillis()}"); put("thread_id", tid); put("sender_id", me)
            put("body", body); put("created_at", System.currentTimeMillis())
        })
        enqueue("message.send", body)
        return tid!!
    }
    fun badges(id: String): List<Badge> {
        val c = readableDatabase.rawQuery("select title, description from badges where profile_id=?", arrayOf(id))
        val out = mutableListOf<Badge>()
        while (c.moveToNext()) out += Badge(c.getString(0), c.getString(1))
        c.close()
        return out
    }
    fun addBadge(id: String, title: String, desc: String) {
        writableDatabase.insert("badges", null, ContentValues().apply {
            put("profile_id", id); put("title", title); put("description", desc)
        })
    }
    fun nodes(): List<Node> {
        val c = readableDatabase.rawQuery("select * from nodes order by id", null)
        val out = mutableListOf<Node>()
        while (c.moveToNext()) out += Node(c.getString(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4), c.getString(5))
        c.close()
        return out
    }
    fun setNode(id: String, status: String) {
        writableDatabase.update("nodes", ContentValues().apply { put("status", status) }, "id=?", arrayOf(id))
    }
    fun pins(): List<Pin> {
        val out = mutableListOf<Pin>()
        listings().forEach { out += Pin(it.id, it.lat, it.lng, it.title, "listing") }
        help().forEach { out += Pin(it.id, it.lat, it.lng, it.title, if (it.kind == "need") "need" else "help") }
        nodes().forEach { out += Pin(it.id, it.lat, it.lng, it.label, "node") }
        val c = readableDatabase.rawQuery("select * from points", null)
        while (c.moveToNext()) out += Pin(c.getString(0), c.getDouble(3), c.getDouble(4), c.getString(1), "point")
        c.close()
        val cr = readableDatabase.rawQuery("select id, lat, lng, body from crisis", null)
        while (cr.moveToNext()) out += Pin(cr.getString(0), cr.getDouble(1), cr.getDouble(2), cr.getString(3), "crisis")
        cr.close()
        return out
    }
    fun addCrisis(author: String, kind: String, body: String, lat: Double?, lng: Double?) {
        writableDatabase.insert("crisis", null, ContentValues().apply {
            put("id", "cr_${System.currentTimeMillis()}"); put("author_id", author); put("kind", kind)
            put("body", body); put("lat", lat ?: 52.23); put("lng", lng ?: 21.01); put("created_at", System.currentTimeMillis())
        })
        addBadge(author, "Strażnik sieci", "Tryb kryzysowy")
        bump(author, 4)
        enqueue("crisis.$kind", body)
    }
    fun enqueue(action: String, payload: String) {
        writableDatabase.insert("queue", null, ContentValues().apply {
            put("id", "q_${System.currentTimeMillis()}"); put("action", action); put("payload", payload)
            put("created_at", System.currentTimeMillis()); put("synced", 0)
        })
    }
    fun queue(): List<QueueItem> {
        val c = readableDatabase.rawQuery("select id, action, payload, synced from queue order by created_at desc limit 40", null)
        val out = mutableListOf<QueueItem>()
        while (c.moveToNext()) out += QueueItem(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1)
        c.close()
        return out
    }
    fun flushQueue() {
        writableDatabase.update("queue", ContentValues().apply { put("synced", 1) }, "synced=0", null)
    }
    private fun bump(id: String, d: Int) {
        writableDatabase.execSQL("update users set reputation = reputation + ? where id=?", arrayOf(d, id))
    }
    private fun lastMsg(threadId: String): Msg? {
        val c = readableDatabase.rawQuery("select * from messages where thread_id=? order by created_at desc limit 1", arrayOf(threadId))
        val m = if (c.moveToFirst()) Msg(c.getString(0), threadId, c.getString(2), "", c.getString(3), c.getLong(4)) else null
        c.close()
        return m
    }
    private fun android.database.Cursor.toProfile(): Profile? = if (moveToFirst()) readProfile() else null
    private fun android.database.Cursor.readProfile() = Profile(
        getString(getColumnIndexOrThrow("id")),
        getString(getColumnIndexOrThrow("name")),
        getString(getColumnIndexOrThrow("bio")),
        getString(getColumnIndexOrThrow("district")),
        getDouble(getColumnIndexOrThrow("lat")),
        getDouble(getColumnIndexOrThrow("lng")),
        getInt(getColumnIndexOrThrow("reputation")),
        getInt(getColumnIndexOrThrow("hue")),
        getString(getColumnIndexOrThrow("password")),
    )
}
