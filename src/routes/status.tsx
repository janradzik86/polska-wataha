import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { inspectAdapters } from "@/lib/comm/adapters";
import { flushQueue, listQueue, queueOffline } from "@/lib/server/siatka";
import { isEffectivelyOnline, useSiatka } from "@/lib/store";

export const Route = createFileRoute("/status")({ component: Page });

function Page() {
  return (
    <Guard>
      <Status />
    </Guard>
  );
}

function Status() {
  const simulate = useSiatka((s) => s.simulateOffline);
  const setSimulate = useSiatka((s) => s.setSimulateOffline);
  const queue = useSiatka((s) => s.queue);
  const clearQueue = useSiatka((s) => s.clearQueue);
  const online = isEffectivelyOnline();
  const adapters = inspectAdapters(online);
  const remote = useQuery({ queryKey: ["queue"], queryFn: () => listQueue() });

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Warstwa komunikacji</p>
        <h1 className="font-display text-3xl font-semibold">Status sieci</h1>
      </div>
      <div className="rounded-xl border border-border bg-surface p-4">
        <p className="text-sm text-muted">
          Aplikacja → Communication Service → Adapter → Internet / BLE / Heltec SX1262 (EU868)
        </p>
        <div className="mt-3 flex items-center justify-between gap-3">
          <p className="text-sm">{simulate ? "Symulacja braku internetu włączona" : "Symulacja wyłączona"}</p>
          <Button variant={simulate ? "danger" : "secondary"} onClick={() => setSimulate(!simulate)}>
            {simulate ? "Włącz sieć" : "Testuj offline"}
          </Button>
        </div>
      </div>
      <div className="grid gap-3">
        {adapters.map((a) => (
          <div key={a.id} className="rounded-xl border border-border bg-surface p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-mono text-sm">{a.label}</p>
                <p className="text-xs uppercase tracking-wider text-subtle">{a.layer}</p>
              </div>
              <Badge variant={a.health === "up" ? "ok" : a.health === "down" ? "danger" : "warn"}>
                {a.health}
              </Badge>
            </div>
            <p className="mt-2 text-sm text-muted">{a.detail}</p>
            <p className="mt-1 text-[11px] text-subtle">{a.stage}</p>
          </div>
        ))}
      </div>
      <div className="rounded-xl border border-border bg-surface p-4 space-y-3">
        <h2 className="font-display text-lg font-semibold">Kolejka synchronizacji</h2>
        <p className="text-sm text-muted">
          Wpisy lokalne: {queue.length}. Serwer: {(remote.data ?? []).filter((r) => !r.synced_at).length} oczekujących.
        </p>
        <div className="flex flex-wrap gap-2">
          <Button
            variant="secondary"
            onClick={async () => {
              await queueOffline({
                data: { action: "ping.offline", payload: JSON.stringify({ t: Date.now() }) },
              });
              await remote.refetch();
            }}
          >
            Dodaj wpis testowy
          </Button>
          <Button
            onClick={async () => {
              await flushQueue();
              clearQueue();
              await remote.refetch();
            }}
          >
            Synchronizuj
          </Button>
        </div>
        <ul className="space-y-1 text-xs text-muted">
          {queue.map((i) => (
            <li key={i.id} className="font-mono">
              local · {i.action}
            </li>
          ))}
          {(remote.data ?? []).slice(0, 8).map((i) => (
            <li key={i.id} className="font-mono">
              {i.synced_at ? "synced" : "queued"} · {i.action}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
