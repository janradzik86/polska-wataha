import { getSql } from "@/lib/db";
import { DEMO_EMAIL } from "@/lib/siatka";

const SEEDED = "siatka_seed_v1";

export async function ensureSeed() {
  const sql = await getSql();
  const flag = await sql<{ n: number }>`select count(*)::int as n from badges`;
  if ((flag[0]?.n ?? 0) > 0) return;

  await sql`
    insert into badges (id, slug, title, description) values
    ('bdg_first', 'pierwszy-post', 'Pierwszy post', 'Opublikowano pierwsze ogłoszenie'),
    ('bdg_helper', 'pomocnik', 'Pomocnik', 'Zaoferowano pomoc sąsiadowi'),
    ('bdg_neighbor', 'sasiad', 'Sąsiad roku', 'Wysoka reputacja w dzielnicy'),
    ('bdg_guard', 'straznik', 'Strażnik sieci', 'Udział w trybie kryzysowym'),
    ('bdg_offline', 'offline', 'Offline hero', 'Działanie bez internetu'),
    ('bdg_exchange', 'wymiana', 'Wymieniacz', 'Zaproponowano wymianę'),
    ('bdg_give', 'oddajacy', 'Dawca', 'Oddano rzecz za darmo')
  `;

  await sql`
    insert into profiles (id, user_id, display_name, bio, district, lat, lng, reputation, avatar_hue, is_seed) values
    ('p_marek', null, 'Marek Nowak', 'Stolarz z Woli. Pożyczam narzędzia, naprawiam meble.', 'Wola', 52.231, 20.984, 42, 28, true),
    ('p_ewa', null, 'Ewa Wiśniewska', 'Pielęgniarka. Dyżury nocne, chętnie pomogę seniorom.', 'Mokotów', 52.201, 21.017, 61, 200, true),
    ('p_piotr', null, 'Piotr Zieliński', 'Rowerzysta, kurier. Znam każdy skrót na Pradze.', 'Praga-Północ', 52.256, 21.035, 33, 140, true),
    ('p_lena', null, 'Lena Kruk', 'Ogrodniczka. Sadzonki, kompost, wiedza o glebie.', 'Żoliborz', 52.269, 20.986, 54, 90, true),
    ('p_jan', null, 'Jan Olszewski', 'Emerytowany elektryk. Drobne naprawy w bloku.', 'Ochota', 52.214, 20.981, 47, 320, true)
  `;

  await sql`
    insert into listings (id, author_id, kind, title, body, category, lat, lng, district, status) values
    ('l1', 'p_marek', 'offer', 'Wiertarka udarowa na weekend', 'Bosch 800W, walizka, wiertła do betonu. Oddam za słoik miodu albo przysługę.', 'Narzędzia', 52.231, 20.984, 'Wola', 'open'),
    ('l2', 'p_lena', 'giveaway', 'Sadzonki pomidorów Malinowy Ożarski', 'Dwanaście sztuk, zahartowane. Odbór na Żoliborzu, doniczki zwrotne.', 'Ogród', 52.269, 20.986, 'Żoliborz', 'open'),
    ('l3', 'p_ewa', 'want', 'Szukam wózka dziecięcego', 'Na kilka miesięcy, dziecko znajomych. Stan używany OK.', 'Dzieci', 52.201, 21.017, 'Mokotów', 'open'),
    ('l4', 'p_piotr', 'exchange', 'Rower miejski za hulajnogę', 'Kross Trans 5.0, 28 cali. Szukam hulajnogi elektrycznej do 25 km/h.', 'Transport', 52.256, 21.035, 'Praga-Północ', 'open'),
    ('l5', 'p_jan', 'offer', 'Sprawdzenie instalacji w mieszkaniu', 'Bezpieczniki, gniazdka, oświetlenie. Herbata mile widziana.', 'Dom', 52.214, 20.981, 'Ochota', 'open'),
    ('l6', 'p_lena', 'giveaway', 'Słoiki 0,9 l — 24 sztuki', 'Wykiprowane, z zakrętkami. Pod przetwory.', 'Dom', 52.268, 20.988, 'Żoliborz', 'open'),
    ('l7', 'p_marek', 'offer', 'Przyczepka rowerowa', 'Na dwa rowery albo zakupy. Weekendowo.', 'Transport', 52.233, 20.987, 'Wola', 'open'),
    ('l8', 'p_ewa', 'giveaway', 'Odzież dziecięca 98–104', 'Kurtka, spodnie, swetry. Czyste, z metkami prania.', 'Odzież', 52.203, 21.02, 'Mokotów', 'open')
  `;

  await sql`
    insert into help_posts (id, author_id, kind, title, body, urgency, lat, lng, district, status) values
    ('h1', 'p_jan', 'need', 'Zakupy dla osoby starszej', 'Nie wychodzę dalej niż klatka. Lista krótka: chleb, mleko, leki z apteki na Banacha.', 'high', 52.214, 20.981, 'Ochota', 'open'),
    ('h2', 'p_ewa', 'offer', 'Opieka po zabiegu — dyżur wieczorny', 'Mogę zostać 3–4 godziny, zmierzyć ciśnienie, podać leki wg listy.', 'normal', 52.201, 21.017, 'Mokotów', 'open'),
    ('h3', 'p_piotr', 'offer', 'Transport na SOR / przychodnię', 'Auto, fotelik na życzenie. Praga i Śródmieście.', 'high', 52.256, 21.035, 'Praga-Północ', 'open'),
    ('h4', 'p_lena', 'need', 'Podlewanie ogrodu 12–18 maja', 'Wyjazd. Dwa grządki i donice na balkonie.', 'low', 52.269, 20.986, 'Żoliborz', 'open'),
    ('h5', 'p_marek', 'offer', 'Wniesienie mebli / pomoc przy przeprowadzce', 'Weekend, dwie pary rąk jeśli dam znać wcześniej.', 'normal', 52.231, 20.984, 'Wola', 'open')
  `;

  await sql`
    insert into help_points (id, title, kind, lat, lng, note) values
    ('hp1', 'Punkt wody — Plac Wilsona', 'water', 52.269, 20.986, 'Kran miejski, 24h'),
    ('hp2', 'Punkt medyczny — Szpital Dzieciątka Jezus', 'medical', 52.226, 21.012, 'Izba przyjęć'),
    ('hp3', 'Schronienie — Centrum Targowa', 'shelter', 52.251, 21.038, 'Sala gimnastyczna, 80 miejsc'),
    ('hp4', 'Punkt żywności — Hala Mirowska', 'food', 52.239, 20.998, 'Paczki żywnościowe 9–14')
  `;

  await sql`
    insert into mesh_nodes (id, label, kind, lat, lng, status) values
    ('NODE_A', 'NODE A — Śródmieście', 'phone', 52.2297, 21.0122, 'online'),
    ('NODE_B', 'NODE B — Wola', 'wifi', 52.231, 20.984, 'online'),
    ('NODE_C', 'NODE C — Praga', 'bt', 52.256, 21.035, 'online'),
    ('NODE_D', 'NODE D — Heltec V4 EU868', 'lora', 52.201, 21.017, 'online')
  `;

  await sql`
    insert into profile_badges (profile_id, badge_id) values
    ('p_marek', 'bdg_helper'),
    ('p_marek', 'bdg_exchange'),
    ('p_ewa', 'bdg_neighbor'),
    ('p_ewa', 'bdg_helper'),
    ('p_piotr', 'bdg_offline'),
    ('p_lena', 'bdg_give'),
    ('p_lena', 'bdg_first'),
    ('p_jan', 'bdg_guard'),
    ('p_jan', 'bdg_helper')
  `;

  await sql`
    insert into crisis_alerts (id, author_id, kind, body, lat, lng) values
    ('c1', 'p_jan', 'broadcast', 'Ćwiczenie: komunikat testowy sieci Polska Wataha. Brak realnego zagrożenia.', 52.214, 20.981),
    ('c2', 'p_ewa', 'can_help', 'Punkt opatrunkowy gotowy na Mokotowie — ćwiczenie.', 52.201, 21.017)
  `;

  void SEEDED;
}

export async function ensureProfile(userId: string, name: string, email: string) {
  const sql = await getSql();
  await ensureSeed();
  const existing = await sql<{ id: string }>`select id from profiles where user_id = ${userId} limit 1`;
  if (existing[0]) return existing[0].id;

  const id = `p_${userId.slice(0, 12)}`;
  const display = name?.trim() || email.split("@")[0] || "Sąsiad";
  const isDemo = email === DEMO_EMAIL;
  await sql`
    insert into profiles (id, user_id, display_name, bio, district, lat, lng, reputation, avatar_hue, is_seed)
    values (${id}, ${userId}, ${display}, ${isDemo ? "Konto demonstracyjne Siatki. Mokotów / Śródmieście." : ""},
      ${isDemo ? "Mokotów" : "Śródmieście"}, ${52.22}, ${21.01}, ${isDemo ? 18 : 0}, ${isDemo ? 48 : 175}, false)
  `;

  if (isDemo) {
    await sql`insert into profile_badges (profile_id, badge_id) values (${id}, 'bdg_first') on conflict do nothing`;
    await sql`insert into profile_badges (profile_id, badge_id) values (${id}, 'bdg_offline') on conflict do nothing`;
    await sql`insert into reputation_events (id, profile_id, delta, reason) values (${id + "_rep"}, ${id}, 18, 'Konto demonstracyjne')`;

    const threadId = `th_demo_${id}`;
    const hasThread = await sql<{ n: number }>`select count(*)::int as n from threads where id = ${threadId}`;
    if ((hasThread[0]?.n ?? 0) === 0) {
      await sql`insert into threads (id, listing_id, title) values (${threadId}, 'l1', 'Wiertarka — Marek')`;
      await sql`insert into thread_members (thread_id, profile_id) values (${threadId}, ${id}), (${threadId}, 'p_marek')`;
      await sql`
        insert into messages (id, thread_id, sender_id, body) values
        (${threadId + "_m1"}, ${threadId}, 'p_marek', 'Cześć! Wiertarka wolna w sobotę od 10. Dam zestaw wierteł.'),
        (${threadId + "_m2"}, ${threadId}, ${id}, 'Dzięki, biorę. Wpadnę koło 10:30.')
      `;
    }
  }
  return id;
}
