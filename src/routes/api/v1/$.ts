import { createFileRoute } from "@tanstack/react-router";
import { getSql } from "@/lib/db";
import { ensureSeed } from "@/lib/server/seed";

async function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "access-control-allow-origin": "*",
      "access-control-allow-headers": "content-type, x-siatka-token, authorization",
      "access-control-allow-methods": "GET,POST,OPTIONS",
    },
  });
}

export const Route = createFileRoute("/api/v1/$")({
  server: {
    handlers: {
      OPTIONS: () => json({ ok: true }),
      GET: async ({ request }) => handle(request),
      POST: async ({ request }) => handle(request),
    },
  },
});

async function handle(request: Request) {
  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/api\/v1\/?/, "").replace(/\/$/, "") || "health";
  await ensureSeed();
  const sql = await getSql();

  if (path === "health") {
    return json({
      ok: true,
      app: "polska-wataha",
      version: "0.2.0",
      adapters: ["internet", "bluetooth", "wifi_direct", "lora"],
    });
  }

  if (path === "listings" && request.method === "GET") {
    const rows = await sql`
      select l.*, p.display_name as author_name, p.avatar_hue as author_hue
      from listings l join profiles p on p.id = l.author_id
      where l.status = 'open' order by l.created_at desc limit 80
    `;
    return json({ listings: rows });
  }

  if (path === "help" && request.method === "GET") {
    const rows = await sql`
      select h.*, p.display_name as author_name
      from help_posts h join profiles p on p.id = h.author_id
      where h.status = 'open' order by h.created_at desc
    `;
    return json({ help: rows });
  }

  if (path === "people" && request.method === "GET") {
    const rows = await sql`select id, display_name, bio, district, reputation, avatar_hue, lat, lng from profiles order by reputation desc`;
    return json({ people: rows });
  }

  if (path === "mesh" && request.method === "GET") {
    const rows = await sql`select * from mesh_nodes order by id`;
    return json({ nodes: rows });
  }

  if (path === "crisis" && request.method === "GET") {
    const rows = await sql`
      select c.*, p.display_name as author_name
      from crisis_alerts c join profiles p on p.id = c.author_id
      order by c.created_at desc limit 40
    `;
    return json({ crisis: rows });
  }

  if (path === "map" && request.method === "GET") {
    const listings = await sql`select id, title, kind, lat, lng from listings where lat is not null`;
    const help = await sql`select id, title, kind, lat, lng from help_posts where lat is not null`;
    const nodes = await sql`select * from mesh_nodes`;
    const points = await sql`select * from help_points`;
    return json({ listings, help, nodes, points });
  }

  return json({ error: "not_found", path }, 404);
}
