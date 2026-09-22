import { useMemo, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { MapBoard } from "@/components/map-board";
import { Button } from "@/components/ui/button";
import { mapPayload } from "@/lib/server/siatka";
import type { MapPin } from "@/lib/siatka";
import { mayParentSee } from "@/lib/family/access";

export const Route = createFileRoute("/map")({ component: Page });

function Page() {
  return (
    <Guard>
      <MapView />
    </Guard>
  );
}

function MapView() {
  const q = useQuery({ queryKey: ["map"], queryFn: () => mapPayload() });
  const [me, setMe] = useState<{ lat: number; lng: number } | null>(null);
  const [filter, setFilter] = useState<string>("all");
  const data = q.data;

  const pins: MapPin[] = useMemo(() => {
    if (!data) return [];
    const list: MapPin[] = [];
    for (const l of data.listings) {
      if (l.lat && l.lng)
        list.push({
          id: l.id,
          lat: l.lat,
          lng: l.lng,
          label: l.title,
          tone: "listing",
          href: `/listings/${l.id}`,
        });
    }
    for (const h of data.helpPins) {
      list.push({
        id: h.id,
        lat: h.lat,
        lng: h.lng,
        label: h.title,
        tone: h.kind === "need" ? "need" : "help",
        href: "/help",
      });
    }
    for (const n of data.nodes) {
      list.push({ id: n.id, lat: n.lat, lng: n.lng, label: n.label, tone: "node", href: "/mesh" });
    }
    for (const p of data.points) {
      list.push({ id: p.id, lat: p.lat, lng: p.lng, label: p.title, tone: "point" });
    }
    for (const c of data.crisis) {
      list.push({ id: c.id, lat: c.lat, lng: c.lng, label: c.body, tone: "crisis" });
    }
    if (filter === "all") return list;
    return list.filter((p) => p.tone === filter);
  }, [data, filter]);

  function locate() {
    navigator.geolocation?.getCurrentPosition(
      (pos) => setMe({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => setMe({ lat: 52.2297, lng: 21.0122 }),
    );
  }

  const adminBlocked = mayParentSee({
    viewerId: "admin",
    childId: "dziecko",
    viewerIsAdmin: true,
    linkStatus: "active",
    parentUserId: "rodzic",
    consent: "while_app_open",
  });

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between gap-3">
        <div>
          <p className="flex items-center gap-1.5 text-xs uppercase tracking-[0.2em] text-primary">
            Czarne Wilki · mapa watahy
          </p>
          <h1 className="font-display text-3xl font-semibold">Warszawa</h1>
        </div>
        <Button variant="secondary" onClick={locate}>
          Moja lokalizacja
        </Button>
      </div>
      <p className="text-sm text-muted">
        Mapa działa bez klucza Google. Piny: ogłoszenia, pomoc, potrzeby, punkty, węzły, kryzys.
      </p>
      <div className="flex flex-wrap gap-2">
        {["all", "listing", "help", "need", "point", "node", "crisis"].map((f) => (
          <Button key={f} size="sm" variant={filter === f ? "default" : "outline"} onClick={() => setFilter(f)}>
            {f === "all" ? "Wszystko" : f}
          </Button>
        ))}
      </div>
      <MapBoard pins={pins} me={me} />
      <section className="rounded-2xl border border-border p-4">
        <h2 className="font-display text-xl">Dziecko</h2>
        <p className="mt-2 text-sm text-muted">
          Pozycja dziecka przychodzi tylko z aktywnego parent_link i za zgodą.
          {adminBlocked.ok ? " " : " Administrator bez tego linku jej nie dostaje."}
          Nie ma jeszcze żywej pozycji z mostu, więc przyciski nie rysują dziecka.
        </p>
        <div className="mt-3 flex flex-wrap gap-2">
          <Button type="button" variant="secondary" disabled>
            Pokaż na mapie
          </Button>
          <Button type="button" variant="secondary" disabled>
            Nawiguj do dziecka
          </Button>
        </div>
        <p className="mt-2 text-xs text-muted">BRouter nie jest podłączony. Nawigacja ulicami nie wystartuje.</p>
      </section>
    </div>
  );
}
