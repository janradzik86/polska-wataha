import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { authMiddleware } from "@/lib/auth/middleware";
import { getSql } from "@/lib/db";
import { uid } from "@/lib/utils";
import { ensureProfile, ensureSeed } from "./seed";
import type {
  ChatMessage,
  CrisisAlert,
  ExchangeOffer,
  HelpPoint,
  HelpPost,
  Listing,
  MeshNode,
  Profile,
  ThreadSummary,
} from "@/lib/siatka";

async function me(userId: string, name?: string, email?: string) {
  const sql = await getSql();
  const rows = await sql<{ name: string; email: string }>`
    select name, email from "user" where id = ${userId} limit 1
  `;
  const display = name || rows[0]?.name || "Sąsiad";
  const mail = email || rows[0]?.email || "";
  return ensureProfile(userId, display, mail);
}

const listingRow = (r: Record<string, unknown>): Listing => ({
  id: String(r.id),
  authorId: String(r.author_id),
  authorName: String(r.author_name ?? ""),
  authorHue: Number(r.author_hue ?? 160),
  kind: r.kind as Listing["kind"],
  title: String(r.title),
  body: String(r.body),
  category: String(r.category),
  lat: r.lat == null ? null : Number(r.lat),
  lng: r.lng == null ? null : Number(r.lng),
  district: String(r.district ?? ""),
  status: String(r.status),
  createdAt: String(r.created_at),
});

export const bootstrap = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .handler(async ({ context }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const [profile] = await sql<Record<string, unknown>>`
      select * from profiles where id = ${profileId}
    `;
    const badges = await sql<{ id: string; slug: string; title: string; description: string; earned_at: string }>`
      select b.id, b.slug, b.title, b.description, pb.earned_at
      from profile_badges pb join badges b on b.id = pb.badge_id
      where pb.profile_id = ${profileId}
    `;
    return {
      profileId,
      profile: {
        id: String(profile.id),
        userId: profile.user_id ? String(profile.user_id) : null,
        displayName: String(profile.display_name),
        bio: String(profile.bio),
        district: String(profile.district),
        lat: profile.lat == null ? null : Number(profile.lat),
        lng: profile.lng == null ? null : Number(profile.lng),
        reputation: Number(profile.reputation),
        avatarHue: Number(profile.avatar_hue),
        isSeed: Boolean(profile.is_seed),
        createdAt: String(profile.created_at),
        badges: badges.map((b) => ({
          id: b.id,
          slug: b.slug,
          title: b.title,
          description: b.description,
          earnedAt: String(b.earned_at),
        })),
      } satisfies Profile,
    };
  });

export const listListings = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      q: z.string().optional(),
      kind: z.string().optional(),
      category: z.string().optional(),
    }),
  )
  .handler(async ({ data }) => {
    await ensureSeed();
    const sql = await getSql();
    const q = data.q?.trim() ? `%${data.q.trim().toLowerCase()}%` : null;
    const rows = await sql<Record<string, unknown>>`
      select l.*, p.display_name as author_name, p.avatar_hue as author_hue
      from listings l join profiles p on p.id = l.author_id
      where l.status = 'open'
        and (${data.kind ?? null}::text is null or l.kind = ${data.kind ?? null})
        and (${data.category ?? null}::text is null or l.category = ${data.category ?? null})
        and (${q}::text is null or lower(l.title) like ${q} or lower(l.body) like ${q} or lower(l.district) like ${q})
      order by l.created_at desc
      limit 80
    `;
    return rows.map(listingRow);
  });

export const getListing = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .validator(z.object({ id: z.string() }))
  .handler(async ({ data }) => {
    const sql = await getSql();
    const rows = await sql<Record<string, unknown>>`
      select l.*, p.display_name as author_name, p.avatar_hue as author_hue
      from listings l join profiles p on p.id = l.author_id
      where l.id = ${data.id}
    `;
    return rows[0] ? listingRow(rows[0]) : null;
  });

export const createListing = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      kind: z.enum(["offer", "want", "giveaway", "exchange"]),
      title: z.string().min(3).max(120),
      body: z.string().min(3).max(2000),
      category: z.string().min(1).max(40),
      district: z.string().max(40).optional(),
      lat: z.number().optional(),
      lng: z.number().optional(),
      offline: z.boolean().optional(),
    }),
  )
  .handler(async ({ context, data }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const id = uid("lst");
    await sql`
      insert into listings (id, author_id, kind, title, body, category, lat, lng, district, status)
      values (${id}, ${profileId}, ${data.kind}, ${data.title}, ${data.body}, ${data.category},
        ${data.lat ?? 52.2297}, ${data.lng ?? 21.0122}, ${data.district ?? "Śródmieście"}, 'open')
    `;
    await sql`
      insert into profile_badges (profile_id, badge_id)
      values (${profileId}, ${data.kind === "giveaway" ? "bdg_give" : "bdg_first"})
      on conflict do nothing
    `;
    await sql`
      insert into reputation_events (id, profile_id, delta, reason)
      values (${uid("rep")}, ${profileId}, 2, 'Nowe ogłoszenie')
    `;
    await sql`update profiles set reputation = reputation + 2 where id = ${profileId}`;
    if (data.offline) {
      await sql`
        insert into sync_queue (id, user_id, action, payload)
        values (${uid("q")}, ${context.userId}, 'listing.create', ${JSON.stringify({ id })})
      `;
    }
    return { id };
  });

export const listHelp = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async () => {
    await ensureSeed();
    const sql = await getSql();
    const rows = await sql<Record<string, unknown>>`
      select h.*, p.display_name as author_name, p.avatar_hue as author_hue
      from help_posts h join profiles p on p.id = h.author_id
      where h.status = 'open'
      order by case h.urgency when 'crisis' then 0 when 'high' then 1 when 'normal' then 2 else 3 end, h.created_at desc
    `;
    return rows.map(
      (r): HelpPost => ({
        id: String(r.id),
        authorId: String(r.author_id),
        authorName: String(r.author_name),
        authorHue: Number(r.author_hue),
        kind: r.kind as HelpPost["kind"],
        title: String(r.title),
        body: String(r.body),
        urgency: r.urgency as HelpPost["urgency"],
        lat: r.lat == null ? null : Number(r.lat),
        lng: r.lng == null ? null : Number(r.lng),
        district: String(r.district),
        status: String(r.status),
        createdAt: String(r.created_at),
      }),
    );
  });

export const createHelp = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      kind: z.enum(["need", "offer"]),
      title: z.string().min(3).max(120),
      body: z.string().min(3).max(2000),
      urgency: z.enum(["low", "normal", "high", "crisis"]).default("normal"),
      district: z.string().optional(),
      lat: z.number().optional(),
      lng: z.number().optional(),
    }),
  )
  .handler(async ({ context, data }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const id = uid("hlp");
    await sql`
      insert into help_posts (id, author_id, kind, title, body, urgency, lat, lng, district)
      values (${id}, ${profileId}, ${data.kind}, ${data.title}, ${data.body}, ${data.urgency},
        ${data.lat ?? 52.2297}, ${data.lng ?? 21.0122}, ${data.district ?? "Śródmieście"})
    `;
    if (data.kind === "offer") {
      await sql`insert into profile_badges (profile_id, badge_id) values (${profileId}, 'bdg_helper') on conflict do nothing`;
      await sql`update profiles set reputation = reputation + 3 where id = ${profileId}`;
    }
    return { id };
  });

export const listPeople = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async () => {
    await ensureSeed();
    const sql = await getSql();
    const rows = await sql<Record<string, unknown>>`
      select * from profiles order by reputation desc, display_name
    `;
    const badges = await sql<{ profile_id: string; id: string; slug: string; title: string; description: string; earned_at: string }>`
      select pb.profile_id, b.id, b.slug, b.title, b.description, pb.earned_at
      from profile_badges pb join badges b on b.id = pb.badge_id
    `;
    const byP = new Map<string, Profile["badges"]>();
    for (const b of badges) {
      const list = byP.get(b.profile_id) ?? [];
      list.push({
        id: b.id,
        slug: b.slug,
        title: b.title,
        description: b.description,
        earnedAt: String(b.earned_at),
      });
      byP.set(b.profile_id, list);
    }
    return rows.map(
      (r): Profile => ({
        id: String(r.id),
        userId: r.user_id ? String(r.user_id) : null,
        displayName: String(r.display_name),
        bio: String(r.bio),
        district: String(r.district),
        lat: r.lat == null ? null : Number(r.lat),
        lng: r.lng == null ? null : Number(r.lng),
        reputation: Number(r.reputation),
        avatarHue: Number(r.avatar_hue),
        isSeed: Boolean(r.is_seed),
        createdAt: String(r.created_at),
        badges: byP.get(String(r.id)) ?? [],
      }),
    );
  });

export const getPerson = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .validator(z.object({ id: z.string() }))
  .handler(async ({ data }) => {
    const people = await listPeople();
    return people.find((p) => p.id === data.id) ?? null;
  });

export const listThreads = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async ({ context }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const rows = await sql<Record<string, unknown>>`
      select t.id, t.title, t.listing_id,
        (select body from messages m where m.thread_id = t.id order by created_at desc limit 1) as last_body,
        (select created_at from messages m where m.thread_id = t.id order by created_at desc limit 1) as last_at,
        (select p.display_name from thread_members tm join profiles p on p.id = tm.profile_id
          where tm.thread_id = t.id and tm.profile_id <> ${profileId} limit 1) as peer_name,
        (select p.avatar_hue from thread_members tm join profiles p on p.id = tm.profile_id
          where tm.thread_id = t.id and tm.profile_id <> ${profileId} limit 1) as peer_hue
      from threads t
      join thread_members me on me.thread_id = t.id and me.profile_id = ${profileId}
      order by last_at desc nulls last
    `;
    return rows.map(
      (r): ThreadSummary => ({
        id: String(r.id),
        title: String(r.title),
        listingId: r.listing_id ? String(r.listing_id) : null,
        lastBody: String(r.last_body ?? ""),
        lastAt: String(r.last_at ?? r.created_at ?? ""),
        peerName: String(r.peer_name ?? "Sąsiad"),
        peerHue: Number(r.peer_hue ?? 160),
      }),
    );
  });

export const getMessages = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .validator(z.object({ threadId: z.string() }))
  .handler(async ({ context, data }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const member = await sql<{ profile_id: string }>`
      select profile_id from thread_members where thread_id = ${data.threadId} and profile_id = ${profileId}
    `;
    if (!member[0]) return [] as ChatMessage[];
    const rows = await sql<Record<string, unknown>>`
      select m.*, p.display_name as sender_name
      from messages m join profiles p on p.id = m.sender_id
      where m.thread_id = ${data.threadId}
      order by m.created_at asc
    `;
    return rows.map(
      (r): ChatMessage => ({
        id: String(r.id),
        threadId: String(r.thread_id),
        senderId: String(r.sender_id),
        senderName: String(r.sender_name),
        body: String(r.body),
        createdAt: String(r.created_at),
      }),
    );
  });

export const sendMessage = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      threadId: z.string().optional(),
      toProfileId: z.string().optional(),
      listingId: z.string().optional(),
      body: z.string().min(1).max(2000),
    }),
  )
  .handler(async ({ context, data }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    let threadId = data.threadId;
    if (!threadId) {
      if (!data.toProfileId) throw new Error("Brak odbiorcy");
      const existing = await sql<{ thread_id: string }>`
        select tm.thread_id from thread_members tm
        join thread_members tm2 on tm2.thread_id = tm.thread_id and tm2.profile_id = ${data.toProfileId}
        where tm.profile_id = ${profileId}
        limit 1
      `;
      if (existing[0]) threadId = existing[0].thread_id;
      else {
        threadId = uid("th");
        const peer = await sql<{ display_name: string }>`select display_name from profiles where id = ${data.toProfileId}`;
        await sql`insert into threads (id, listing_id, title) values (${threadId}, ${data.listingId ?? null}, ${peer[0]?.display_name ?? "Rozmowa"})`;
        await sql`insert into thread_members (thread_id, profile_id) values (${threadId}, ${profileId}), (${threadId}, ${data.toProfileId})`;
      }
    }
    const id = uid("msg");
    await sql`insert into messages (id, thread_id, sender_id, body) values (${id}, ${threadId}, ${profileId}, ${data.body})`;
    return { id, threadId };
  });

export const createExchange = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      listingId: z.string(),
      offerText: z.string().min(3).max(500),
    }),
  )
  .handler(async ({ context, data }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const listing = await sql<{ author_id: string; title: string }>`
      select author_id, title from listings where id = ${data.listingId}
    `;
    if (!listing[0]) throw new Error("Ogłoszenie nie istnieje");
    const id = uid("ex");
    await sql`
      insert into exchange_offers (id, listing_id, from_id, to_id, offer_text)
      values (${id}, ${data.listingId}, ${profileId}, ${listing[0].author_id}, ${data.offerText})
    `;
    await sql`insert into profile_badges (profile_id, badge_id) values (${profileId}, 'bdg_exchange') on conflict do nothing`;
    await sql`update profiles set reputation = reputation + 2 where id = ${profileId}`;
    const existing = await sql<{ thread_id: string }>`
      select tm.thread_id from thread_members tm
      join thread_members tm2 on tm2.thread_id = tm.thread_id and tm2.profile_id = ${listing[0].author_id}
      where tm.profile_id = ${profileId}
      limit 1
    `;
    let threadId = existing[0]?.thread_id;
    if (!threadId) {
      threadId = uid("th");
      await sql`insert into threads (id, listing_id, title) values (${threadId}, ${data.listingId}, ${listing[0].title})`;
      await sql`insert into thread_members (thread_id, profile_id) values (${threadId}, ${profileId}), (${threadId}, ${listing[0].author_id})`;
    }
    await sql`insert into messages (id, thread_id, sender_id, body) values (${uid("msg")}, ${threadId}, ${profileId}, ${"Propozycja wymiany: " + data.offerText})`;
    return { id, threadId };
  });

export const listExchanges = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async ({ context }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const rows = await sql<Record<string, unknown>>`
      select e.*, l.title as listing_title, p.display_name as from_name
      from exchange_offers e
      join listings l on l.id = e.listing_id
      join profiles p on p.id = e.from_id
      where e.from_id = ${profileId} or e.to_id = ${profileId}
      order by e.created_at desc
    `;
    return rows.map(
      (r): ExchangeOffer => ({
        id: String(r.id),
        listingId: String(r.listing_id),
        listingTitle: String(r.listing_title),
        fromId: String(r.from_id),
        fromName: String(r.from_name),
        toId: String(r.to_id),
        offerText: String(r.offer_text),
        status: String(r.status),
        createdAt: String(r.created_at),
      }),
    );
  });

export const listCrisis = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async () => {
    await ensureSeed();
    const sql = await getSql();
    const rows = await sql<Record<string, unknown>>`
      select c.*, p.display_name as author_name
      from crisis_alerts c join profiles p on p.id = c.author_id
      order by c.created_at desc limit 40
    `;
    return rows.map(
      (r): CrisisAlert => ({
        id: String(r.id),
        authorId: String(r.author_id),
        authorName: String(r.author_name),
        kind: String(r.kind),
        body: String(r.body),
        lat: r.lat == null ? null : Number(r.lat),
        lng: r.lng == null ? null : Number(r.lng),
        createdAt: String(r.created_at),
      }),
    );
  });

export const postCrisis = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      kind: z.enum(["need_help", "can_help", "broadcast", "location"]),
      body: z.string().min(2).max(500),
      lat: z.number().optional(),
      lng: z.number().optional(),
    }),
  )
  .handler(async ({ context, data }) => {
    const profileId = await me(context.userId);
    const sql = await getSql();
    const id = uid("cr");
    await sql`
      insert into crisis_alerts (id, author_id, kind, body, lat, lng)
      values (${id}, ${profileId}, ${data.kind}, ${data.body}, ${data.lat ?? null}, ${data.lng ?? null})
    `;
    await sql`insert into profile_badges (profile_id, badge_id) values (${profileId}, 'bdg_guard') on conflict do nothing`;
    await sql`update profiles set reputation = reputation + 4 where id = ${profileId}`;
    return { id };
  });

export const mapPayload = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async () => {
    await ensureSeed();
    const sql = await getSql();
    const listings = await sql<Record<string, unknown>>`
      select id, title, kind, lat, lng from listings where lat is not null and status = 'open'
    `;
    const help = await sql<Record<string, unknown>>`
      select id, title, kind, urgency, lat, lng from help_posts where lat is not null and status = 'open'
    `;
    const nodes = await sql<Record<string, unknown>>`select * from mesh_nodes`;
    const points = await sql<Record<string, unknown>>`select * from help_points`;
    const crisis = await sql<Record<string, unknown>>`
      select id, body, kind, lat, lng from crisis_alerts where lat is not null order by created_at desc limit 20
    `;
    return {
      listings: listings.map(listingRow).map((l) => ({
        ...l,
        title: String((listings.find((x) => x.id === l.id) as { title: string }).title),
      })),
      helpPins: help.map((h) => ({
        id: String(h.id),
        title: String(h.title),
        kind: String(h.kind),
        urgency: String(h.urgency),
        lat: Number(h.lat),
        lng: Number(h.lng),
      })),
      nodes: nodes.map(
        (n): MeshNode => ({
          id: String(n.id),
          label: String(n.label),
          kind: n.kind as MeshNode["kind"],
          lat: Number(n.lat),
          lng: Number(n.lng),
          status: n.status as MeshNode["status"],
          lastSeen: String(n.last_seen),
        }),
      ),
      points: points.map(
        (p): HelpPoint => ({
          id: String(p.id),
          title: String(p.title),
          kind: String(p.kind),
          lat: Number(p.lat),
          lng: Number(p.lng),
          note: String(p.note),
        }),
      ),
      crisis: crisis.map((c) => ({
        id: String(c.id),
        body: String(c.body),
        kind: String(c.kind),
        lat: Number(c.lat),
        lng: Number(c.lng),
      })),
    };
  });

export const listNodes = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async () => {
    await ensureSeed();
    const sql = await getSql();
    const nodes = await sql<Record<string, unknown>>`select * from mesh_nodes order by id`;
    return nodes.map(
      (n): MeshNode => ({
        id: String(n.id),
        label: String(n.label),
        kind: n.kind as MeshNode["kind"],
        lat: Number(n.lat),
        lng: Number(n.lng),
        status: n.status as MeshNode["status"],
        lastSeen: String(n.last_seen),
      }),
    );
  });

export const setNodeStatus = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      id: z.string(),
      status: z.enum(["online", "degraded", "down"]),
    }),
  )
  .handler(async ({ data }) => {
    const sql = await getSql();
    await sql`update mesh_nodes set status = ${data.status}, last_seen = now() where id = ${data.id}`;
    return { ok: true };
  });

export const queueOffline = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    z.object({
      action: z.string(),
      payload: z.string(),
    }),
  )
  .handler(async ({ context, data }) => {
    const sql = await getSql();
    const id = uid("q");
    await sql`
      insert into sync_queue (id, user_id, action, payload)
      values (${id}, ${context.userId}, ${data.action}, ${data.payload})
    `;
    const profileId = await me(context.userId);
    await sql`insert into profile_badges (profile_id, badge_id) values (${profileId}, 'bdg_offline') on conflict do nothing`;
    return { id };
  });

export const listQueue = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async ({ context }) => {
    const sql = await getSql();
    return sql<{ id: string; action: string; payload: string; created_at: string; synced_at: string | null }>`
      select id, action, payload, created_at, synced_at from sync_queue
      where user_id = ${context.userId}
      order by created_at desc limit 40
    `;
  });

export const flushQueue = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .handler(async ({ context }) => {
    const sql = await getSql();
    await sql`update sync_queue set synced_at = now() where user_id = ${context.userId} and synced_at is null`;
    return { ok: true };
  });

export const ensureDemoUser = createServerFn({ method: "POST" }).handler(async () => {
  const { auth } = await import("@/lib/auth/server");
  const sql = await getSql();
  const existing = await sql<{ id: string }>`select id from "user" where email = ${"demo@siatka.app"} limit 1`;
  if (existing[0]) return { created: false };
  try {
    await auth.api.signUpEmail({
      body: {
        email: "demo@siatka.app",
        password: "siatka-demo-2026",
        name: "Anna Kowalska",
      },
    });
  } catch {
    // already exists or preview race
  }
  return { created: true };
});
