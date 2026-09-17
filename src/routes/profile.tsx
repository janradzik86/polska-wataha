import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { AvatarMark } from "@/components/avatar-mark";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { bootstrap, listExchanges } from "@/lib/server/siatka";

export const Route = createFileRoute("/profile")({ component: Page });

function Page() {
  return (
    <Guard>
      <Me />
    </Guard>
  );
}

function Me() {
  const me = useQuery({ queryKey: ["boot"], queryFn: () => bootstrap() });
  const ex = useQuery({ queryKey: ["ex"], queryFn: () => listExchanges() });
  const p = me.data?.profile;
  if (!p) return <p className="text-sm text-muted">Wczytywanie profilu…</p>;
  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div className="flex items-center gap-4">
        <AvatarMark name={p.displayName} hue={p.avatarHue} size="lg" />
        <div>
          <h1 className="font-display text-3xl font-semibold">{p.displayName}</h1>
          <p className="text-sm text-muted">
            {p.district} · reputacja {p.reputation}
          </p>
        </div>
      </div>
      <p className="text-sm text-muted">{p.bio}</p>
      <div>
        <h2 className="font-display text-lg font-semibold">Odznaki</h2>
        <div className="mt-2 flex flex-wrap gap-2">
          {p.badges.map((b) => (
            <Badge key={b.id}>
              {b.title}
            </Badge>
          ))}
          {p.badges.length === 0 ? <p className="text-sm text-muted">Jeszcze brak — opublikuj ogłoszenie.</p> : null}
        </div>
      </div>
      <div>
        <h2 className="font-display text-lg font-semibold">Wymiany</h2>
        <div className="mt-2 space-y-2">
          {(ex.data ?? []).map((e) => (
            <div key={e.id} className="rounded-lg border border-border bg-surface p-3 text-sm">
              <p className="font-medium">{e.listingTitle}</p>
              <p className="text-muted">
                {e.fromName}: {e.offerText}
              </p>
              <p className="text-[11px] uppercase text-subtle">{e.status}</p>
            </div>
          ))}
          {(ex.data ?? []).length === 0 ? <p className="text-sm text-muted">Brak propozycji wymiany.</p> : null}
        </div>
      </div>
      <div className="grid gap-2">
        <Button asChild variant="secondary">
          <Link to="/people">Sąsiedzi</Link>
        </Button>
        <Button asChild variant="secondary">
          <Link to="/lora">Radio LoRa · Heltec V4</Link>
        </Button>
        <Button asChild variant="secondary">
          <Link to="/mesh">Mesh Lab</Link>
        </Button>
        <Button asChild variant="secondary">
          <Link to="/status">Status i offline</Link>
        </Button>
        <Button asChild variant="secondary">
          <Link to="/download">Pobierz APK</Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/docs">Architektura</Link>
        </Button>
      </div>
    </div>
  );
}
