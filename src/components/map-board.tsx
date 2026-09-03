import { useMemo, useState } from "react";
import { Link } from "@tanstack/react-router";
import { project, type MapPin } from "@/lib/siatka";
import { cn } from "@/lib/utils";

const TONE: Record<MapPin["tone"], string> = {
  listing: "bg-primary",
  help: "bg-ok",
  need: "bg-warn",
  node: "bg-fg",
  point: "bg-muted",
  me: "bg-danger",
  crisis: "bg-danger",
};

export function MapBoard({
  pins,
  me,
  compact = false,
}: {
  pins: MapPin[];
  me?: { lat: number; lng: number } | null;
  compact?: boolean;
}) {
  const [active, setActive] = useState<string | null>(null);
  const all = useMemo(() => {
    const list = [...pins];
    if (me) list.push({ id: "me", lat: me.lat, lng: me.lng, label: "Tu jesteś", tone: "me" });
    return list;
  }, [pins, me]);
  const selected = all.find((p) => p.id === active);

  return (
    <div className={cn("relative overflow-hidden rounded-xl border border-border bg-elevated", compact ? "h-64" : "h-[28rem]")}>
      <div className="mesh-grid absolute inset-0 opacity-80" />
      <svg className="absolute inset-0 h-full w-full" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden>
        <path
          d="M58 0 C57 18 59 32 56 48 C54 62 57 78 55 100"
          fill="none"
          stroke="currentColor"
          className="text-fg/15"
          strokeWidth="2.2"
        />
        <path
          d="M12 38 H88"
          fill="none"
          stroke="currentColor"
          className="text-fg/10"
          strokeWidth="0.6"
        />
        <path
          d="M40 8 V92"
          fill="none"
          stroke="currentColor"
          className="text-fg/10"
          strokeWidth="0.6"
        />
      </svg>
      <div className="pointer-events-none absolute left-3 top-3 font-display text-[11px] uppercase tracking-[0.18em] text-muted">
        Warszawa · Polska Wataha
      </div>
      <div className="pointer-events-none absolute bottom-3 left-3 right-3 flex flex-wrap gap-2 text-[10px] uppercase tracking-wider text-muted">
        <span>Śródmieście</span>
        <span>Wola</span>
        <span>Mokotów</span>
        <span>Praga</span>
        <span>Żoliborz</span>
      </div>
      {all.map((pin) => {
        const { x, y } = project(pin.lat, pin.lng);
        return (
          <button
            key={pin.id}
            type="button"
            onClick={() => setActive(pin.id)}
            className="absolute -translate-x-1/2 -translate-y-1/2"
            style={{ left: `${x}%`, top: `${y}%` }}
            aria-label={pin.label}
          >
            <span className={cn("block size-2.5 rounded-full ring-2 ring-bg", TONE[pin.tone])} />
          </button>
        );
      })}
      {selected ? (
        <div className="absolute bottom-10 left-3 right-3 rounded-lg border border-border bg-surface/95 p-3 backdrop-blur-sm">
          <p className="text-xs uppercase tracking-wider text-muted">{selected.tone}</p>
          <p className="font-display text-sm font-semibold">{selected.label}</p>
          {selected.href ? (
            <Link to={selected.href} className="mt-1 inline-block text-xs text-primary underline-offset-2 hover:underline">
              Otwórz
            </Link>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
