import { useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { AlertTriangle, HandHelping, Map as MapIcon, Megaphone, Navigation, Radio, X } from "lucide-react";
import { useSiatka } from "@/lib/store";
import { postCrisis } from "@/lib/server/siatka";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { FlagStripe, Kotwica } from "@/components/brand";

const ACTIONS = [
  { id: "need_help", label: "Potrzebuję pomocy", icon: AlertTriangle, kind: "need_help" as const },
  { id: "can_help", label: "Mogę pomóc", icon: HandHelping, kind: "can_help" as const },
  { id: "broadcast", label: "Komunikat", icon: Megaphone, kind: "broadcast" as const },
  { id: "location", label: "Moja lokalizacja", icon: Navigation, kind: "location" as const },
] as const;

export function CrisisOverlay() {
  const crisis = useSiatka((s) => s.crisis);
  const setCrisis = useSiatka((s) => s.setCrisis);
  const navigate = useNavigate();
  const [note, setNote] = useState("");
  const [sent, setSent] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!crisis) return null;

  async function locate(): Promise<{ lat?: number; lng?: number }> {
    if (!navigator.geolocation) return {};
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => resolve({}),
        { enableHighAccuracy: true, timeout: 4000 },
      );
    });
  }

  async function run(kind: (typeof ACTIONS)[number]["kind"], label: string) {
    setBusy(true);
    try {
      const loc = kind === "location" || kind === "need_help" ? await locate() : {};
      const body =
        note.trim() ||
        (kind === "location"
          ? loc.lat
            ? `Lokalizacja ${loc.lat.toFixed(4)}, ${loc.lng?.toFixed(4)}`
            : "Lokalizacja niedostępna — brak uprawnienia"
          : label);
      await postCrisis({ data: { kind, body, ...loc } });
      setSent(label);
    } catch {
      setSent("Zapisano lokalnie — wyślij ponownie przy sieci");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="crisis-root fixed inset-0 z-50 overflow-auto bg-bg text-fg">
      <FlagStripe />
      <div className="mx-auto flex min-h-dvh max-w-lg flex-col px-4 py-6">
        <div className="flex items-center justify-between">
          <p className="flex items-center gap-2 font-display text-sm font-medium uppercase tracking-[0.2em]">
            <Kotwica className="h-3.5 w-3 text-primary" />
            Tryb kryzysowy
          </p>
          <Button variant="ghost" size="icon" onClick={() => setCrisis(false)} aria-label="Zamknij">
            <X />
          </Button>
        </div>
        <h1 className="mt-6 font-display text-4xl font-medium uppercase tracking-wide leading-none">Polska Wataha</h1>
        <p className="mt-2 text-xs uppercase tracking-[0.18em] text-primary">Czarne Wilki Prawdy</p>
        <p className="mt-2 text-sm text-muted">Duże przyciski. Bez animacji. Minimalny pobór.</p>
        <Textarea
          className="mt-6 min-h-20 text-base"
          placeholder="Krótki komunikat (opcjonalnie)"
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
        <div className="mt-4 grid gap-3">
          {ACTIONS.map((a) => (
            <Button key={a.id} variant="crisis" size="xl" disabled={busy} onClick={() => run(a.kind, a.label)}>
              <a.icon className="size-5" />
              {a.label}
            </Button>
          ))}
          <Button
            variant="secondary"
            size="xl"
            onClick={() => {
              setCrisis(false);
              navigate({ to: "/map" });
            }}
          >
            <MapIcon className="size-5" />
            Mapa
          </Button>
          <Button
            variant="secondary"
            size="xl"
            onClick={() => {
              setCrisis(false);
              navigate({ to: "/status" });
            }}
          >
            <Radio className="size-5" />
            Status sieci
          </Button>
        </div>
        {sent ? <p className="mt-4 text-sm text-ok">Wysłano: {sent}</p> : null}
      </div>
    </div>
  );
}
