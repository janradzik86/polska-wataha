import { useMemo, useState } from "react";
import { Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listNodes, setNodeStatus } from "@/lib/server/siatka";
import { routeMesh } from "@/lib/comm/adapters";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const FLOW = ["NODE_A", "NODE_B", "NODE_C", "NODE_D"] as const;

export function MeshLab() {
  const qc = useQueryClient();
  const nodes = useQuery({ queryKey: ["nodes"], queryFn: () => listNodes() });
  const [from, setFrom] = useState("NODE_A");
  const [to, setTo] = useState("NODE_D");
  const mutate = useMutation({
    mutationFn: (p: { id: string; status: "online" | "degraded" | "down" }) => setNodeStatus({ data: p }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nodes"] }),
  });
  const list = nodes.data ?? [];
  const hop = useMemo(() => routeMesh(list, from, to), [list, from, to]);

  return (
    <div className="space-y-5">
      <header className="space-y-1">
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Mesh Lab</p>
        <h1 className="font-display text-3xl font-semibold">Symulator węzłów</h1>
        <p className="max-w-prose text-sm text-muted">
          Cztery węzły A→B→C→D. NODE D to Heltec WiFi LoRa 32 V4 (EU868). Wyłącz NODE B i zobacz
          rerouting. Prawdziwe radio: strona Radio LoRa albo APK.
        </p>
      </header>

      <div className="grid gap-3 sm:grid-cols-2">
        {list.map((n, i) => (
          <div key={n.id} className="rounded-xl border border-border bg-surface p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-display text-lg font-semibold">{n.id.replace("_", " ")}</p>
                <p className="text-xs text-muted">{n.label}</p>
                <p className="mt-1 font-mono text-[11px] uppercase text-subtle">{n.kind}</p>
              </div>
              <Badge variant={n.status === "online" ? "ok" : n.status === "down" ? "danger" : "warn"}>
                {n.status}
              </Badge>
            </div>
            <div className="mt-3 flex gap-2">
              {(["online", "degraded", "down"] as const).map((st) => (
                <Button
                  key={st}
                  size="sm"
                  variant={n.status === st ? "default" : "outline"}
                  onClick={() => mutate.mutate({ id: n.id, status: st })}
                >
                  {st}
                </Button>
              ))}
            </div>
            {i < FLOW.length - 1 ? (
              <p className="mt-3 text-[11px] uppercase tracking-wider text-subtle">następny hop → {FLOW[i + 1]}</p>
            ) : null}
          </div>
        ))}
      </div>

      <div className="rounded-xl border border-border bg-surface p-4">
        <p className="text-xs uppercase tracking-wider text-muted">Przepływ testowy</p>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          {FLOW.map((id, i) => {
            const n = list.find((x) => x.id === id);
            return (
              <div key={id} className="flex items-center gap-2">
                <span
                  className={cn(
                    "rounded-md px-3 py-2 font-mono text-xs",
                    n?.status === "down" ? "bg-danger/20 text-danger" : "bg-elevated",
                  )}
                >
                  {id.replace("_", " ")}
                </span>
                {i < FLOW.length - 1 ? <span className="text-subtle">→</span> : null}
              </div>
            );
          })}
        </div>
        <div className="mt-4 flex flex-wrap gap-3">
          <label className="text-xs text-muted">
            From
            <select
              className="ml-2 h-9 rounded-md border border-border bg-elevated px-2 text-sm text-fg"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            >
              {FLOW.map((id) => (
                <option key={id}>{id}</option>
              ))}
            </select>
          </label>
          <label className="text-xs text-muted">
            To
            <select
              className="ml-2 h-9 rounded-md border border-border bg-elevated px-2 text-sm text-fg"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            >
              {FLOW.map((id) => (
                <option key={id}>{id}</option>
              ))}
            </select>
          </label>
        </div>
        <div className="mt-4 rounded-lg bg-elevated p-3">
          <p className="font-display text-sm font-semibold">{hop.ok ? "Dostarczono" : "Brak trasy"}</p>
          <p className="text-sm text-muted">
            {from} → {hop.via.length ? hop.via.join(" → ") + " → " : ""}
            {to}
          </p>
          {hop.reason ? <p className="mt-1 text-xs text-warn">{hop.reason}</p> : null}
        </div>
        <Button
          className="mt-3"
          variant="secondary"
          onClick={() => mutate.mutate({ id: "NODE_B", status: "down" })}
        >
          Zasymuluj awarię NODE B
        </Button>
        <Link to="/lora" className="mt-3 inline-flex h-11 items-center text-sm text-primary underline-offset-4 hover:underline">
          Otwórz Radio LoRa · Heltec V4
        </Link>
      </div>
    </div>
  );
}
