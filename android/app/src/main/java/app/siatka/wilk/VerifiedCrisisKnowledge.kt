package app.siatka.wilk

/**
 * Konserwatywna baza procedur o podwyższonym ryzyku.
 *
 * Źródła bazowe do audytu treści:
 * - European Resuscitation Council Guidelines 2025
 * - British Red Cross first-aid guidance
 * - CDC emergency drinking-water guidance
 *
 * Ta warstwa ma pierwszeństwo przed starszą bazą SurvivalData dla tematów medycznych
 * i bezpośredniego zagrożenia. Nie diagnozuje i nie dobiera leków.
 */
object VerifiedCrisisKnowledge {
    data class Reply(
        val topic: String,
        val text: String,
        val crisis: Boolean = false
    )

    val knownTopics = setOf(
        "KRYZYS", "Pierwsza pomoc — RKO", "Pierwsza pomoc — krwotok",
        "Pierwsza pomoc — zadławienie", "Pierwsza pomoc — oparzenie",
        "Hipotermia", "Udar cieplny", "Woda — bezpieczeństwo", "Powódź — bezpieczeństwo", "Pierwsza pomoc"
    )

    fun answer(userText: String): Reply? {
        val q = normalize(userText)

        if (containsAny(q, "nie oddycha", "brak oddechu", "nieprzytomny nie oddycha", "rko", "resuscytacja")) {
            return Reply(
                "Pierwsza pomoc — RKO",
                "🚨 Jeśli osoba jest nieprzytomna i nie oddycha prawidłowo: wezwij 112 i poproś o AED. Rozpocznij uciskanie środka klatki piersiowej w tempie 100–120/min, na głębokość około 5–6 cm u osoby dorosłej, pozwalając klatce wracać po każdym ucisku. Jeśli umiesz i możesz: 30 uciśnięć : 2 oddechy. Jeśli nie możesz wykonywać oddechów, wykonuj ciągłe uciśnięcia do przyjazdu pomocy lub użycia AED.",
                crisis = true
            )
        }

        if (containsAny(q, "silny krwotok", "mocno krwawi", "krwotok", "krwawię", "krwawie")) {
            return Reply(
                "Pierwsza pomoc — krwotok",
                "🚨 Przy silnym krwawieniu: zadbaj o własne bezpieczeństwo, mocno i bezpośrednio uciskaj ranę czystym materiałem lub opatrunkiem i wezwij 112, jeśli krwawienie jest ciężkie albo nie ustępuje. Nie zdejmuj pierwszej warstwy, jeśli przesiąknie — dołóż kolejną i dalej uciskaj. Przy zagrażającym życiu krwotoku z kończyny opaska uciskowa może być potrzebna, ale używaj jej zgodnie z instrukcją i najlepiej po przeszkoleniu.",
                crisis = true
            )
        }

        if (containsAny(q, "zadławienie", "zadlawienie", "dusi sie jedzeniem", "nie może oddychać przez jedzenie", "nie moze oddychac przez jedzenie")) {
            return Reply(
                "Pierwsza pomoc — zadławienie",
                "🚨 Jeśli osoba może skutecznie kaszleć, zachęcaj do kaszlu. Jeśli nie może mówić, oddychać ani skutecznie kaszleć, zastosuj do 5 uderzeń między łopatki, a następnie do 5 uciśnięć nadbrzusza u przytomnej osoby dorosłej. Powtarzaj i wezwij 112. Jeśli straci przytomność, rozpocznij RKO i postępuj zgodnie z instrukcjami dyspozytora.",
                crisis = true
            )
        }

        if (containsAny(q, "oparzenie", "poparzenie", "spalilem", "spaliłem", "wrzatek", "wrzątek")) {
            return Reply(
                "Pierwsza pomoc — oparzenie",
                "🩹 Oparzenie chłodź chłodną bieżącą wodą przez co najmniej 20 minut. Zdejmij biżuterię lub luźną odzież w pobliżu, ale nie odrywaj niczego przyklejonego do skóry. Po schłodzeniu przykryj luźno czystą, nieprzylegającą osłoną. Nie używaj lodu, tłuszczu ani kremów. Przy dużym, głębokim oparzeniu, oparzeniu twarzy/dróg oddechowych albo gdy stan budzi niepokój, wezwij pomoc.",
                crisis = false
            )
        }

        if (containsAny(q, "hipotermia", "wychlodzenie", "wychłodzenie", "bardzo zmarzniety", "bardzo zmarznięty")) {
            return Reply(
                "Hipotermia",
                "🧊 Przenieś osobę w osłonięte miejsce, ogranicz dalszą utratę ciepła i jeśli to możliwe usuń mokrą odzież, zastępując ją suchą. Ogrzewaj stopniowo głównie tułów. Nie stosuj bardzo gorącej kąpieli ani intensywnego bezpośredniego źródła ciepła. Przy splątaniu, silnej senności, zaburzeniach oddechu lub utracie przytomności wezwij 112.",
                crisis = false
            )
        }

        if (containsAny(q, "udar cieplny", "udar sloneczny", "udar słoneczny", "splatanie w upale", "splątanie w upale", "bardzo goraco i splatany", "bardzo gorąco i splątany")) {
            return Reply(
                "Udar cieplny",
                "🚨 Podejrzenie udaru cieplnego to stan nagły. Przenieś osobę do chłodnego miejsca, usuń zbędną odzież, rozpocznij szybkie chłodzenie chłodną wodą lub mokrymi okładami i wezwij 112. Nie zostawiaj osoby samej. Jeśli jest nieprzytomna lub nie może bezpiecznie połykać, nie podawaj napojów.",
                crisis = true
            )
        }

        if (containsAny(q, "pierwsza pomoc", "co robic rannemu", "co robić rannemu")) {
            return Reply(
                "Pierwsza pomoc",
                "🩹 Napisz, co dokładnie się stało: krwotok, brak oddechu, zadławienie, oparzenie, złamanie czy inny uraz. Przy bezpośrednim zagrożeniu życia uruchom Tryb Kryzysowy i dzwoń 112, jeśli masz zasięg. WILK poda tylko podstawowe, bezpieczne kroki pierwszej pomocy i nie zastępuje ratownika ani lekarza.",
                crisis = false
            )
        }

        if (containsAny(q, "skażona woda", "skazona woda", "czy gotowac wode", "czy gotować wodę", "woda po powodzi", "jak oczyscic wode", "jak oczyścić wodę", "czy ta woda jest bezpieczna")) {
            return Reply(
                "Woda — bezpieczeństwo",
                "💧 Przy komunikacie o skażeniu wody stosuj dokładnie zalecenia służb lub wodociągów. Gotowanie pomaga przy wielu zagrożeniach biologicznych, ale nie usuwa wielu zanieczyszczeń chemicznych. Jeśli podejrzewane jest skażenie chemiczne lub paliwem, korzystaj z bezpiecznego źródła zastępczego wskazanego przez służby. Wodę po kontakcie z wodą powodziową traktuj jako potencjalnie zanieczyszczoną.",
                crisis = false
            )
        }

        if (containsAny(q, "powódź", "powodz", "woda zalewa", "zalewa dom")) {
            return Reply(
                "Powódź — bezpieczeństwo",
                "🌊 Przy zagrożeniu powodzią przejdź w bezpieczne, wyższe miejsce i stosuj komunikaty służb. Nie wchodź ani nie wjeżdżaj w płynącą lub nieznanej głębokości wodę. Jeśli można to zrobić bezpiecznie i odpowiednio wcześnie, odłącz energię elektryczną zgodnie z zasadami bezpieczeństwa. Po zalaniu nie wchodź do uszkodzonego budynku, dopóki nie ma pewności, że konstrukcja i instalacje są bezpieczne.",
                crisis = false
            )
        }

        if (containsAny(q, "sos", "ratunku", "pomocy!", "napad", "bezpośrednie zagrożenie", "bezposrednie zagrozenie")) {
            return Reply(
                "KRYZYS",
                "🚨 Jeśli jesteś w bezpośrednim zagrożeniu: przejdź w bezpieczne miejsce, jeśli możesz zrobić to bez zwiększania ryzyka. Uruchom Tryb Kryzysowy i SOS w Polskiej Watasze. Jeśli dostępna jest sieć komórkowa, dzwoń pod 112. Podaj co się stało, gdzie jesteś i ile osób potrzebuje pomocy.",
                crisis = true
            )
        }

        return null
    }

    private fun containsAny(q: String, vararg phrases: String): Boolean =
        phrases.any { q.contains(normalize(it)) }

    private fun normalize(s: String): String {
        val nf = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return nf.replace("\\p{Mn}+".toRegex(), "").replace('ł', 'l').replace('Ł', 'L').lowercase()
    }
}
