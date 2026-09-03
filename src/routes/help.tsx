import { useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { AvatarMark } from "@/components/avatar-mark";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { createHelp, listHelp, sendMessage } from "@/lib/server/siatka";
import { HELP_LABEL, type HelpKind, type Urgency } from "@/lib/siatka";
import { timeAgo } from "@/lib/utils";

export const Route = createFileRoute("/help")({ component: Page });

function Page() {
  return (
    <Guard>
      <HelpView />
    </Guard>
  );
}

function HelpView() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const q = useQuery({ queryKey: ["help"], queryFn: () => listHelp() });
  const [kind, setKind] = useState<HelpKind>("need");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [urgency, setUrgency] = useState<Urgency>("normal");

  async function submit() {
    await createHelp({ data: { kind, title, body, urgency } });
    setTitle("");
    setBody("");
    await qc.invalidateQueries({ queryKey: ["help"] });
  }

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Wzajemność</p>
        <h1 className="font-display text-3xl font-semibold">Pomoc</h1>
      </div>
      <div className="rounded-xl border border-border bg-surface p-4 space-y-3">
        <div className="grid grid-cols-2 gap-2">
          <Button variant={kind === "need" ? "default" : "outline"} onClick={() => setKind("need")}>
            Zgłoś potrzebę
          </Button>
          <Button variant={kind === "offer" ? "default" : "outline"} onClick={() => setKind("offer")}>
            Zaoferuj pomoc
          </Button>
        </div>
        <Input placeholder="Tytuł" value={title} onChange={(e) => setTitle(e.target.value)} />
        <Textarea placeholder="Szczegóły" value={body} onChange={(e) => setBody(e.target.value)} />
        <div className="flex gap-2">
          {(["low", "normal", "high", "crisis"] as const).map((u) => (
            <Button key={u} size="sm" variant={urgency === u ? "default" : "outline"} onClick={() => setUrgency(u)}>
              {u}
            </Button>
          ))}
        </div>
        <Button className="w-full" disabled={title.length < 3} onClick={submit}>
          Opublikuj
        </Button>
      </div>
      <div className="grid gap-3">
        {(q.data ?? []).map((h) => (
          <div key={h.id} className="rounded-xl border border-border bg-surface p-4">
            <div className="flex items-start gap-3">
              <AvatarMark name={h.authorName} hue={h.authorHue} />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap gap-2">
                  <Badge variant={h.kind === "need" ? "warn" : "ok"}>{HELP_LABEL[h.kind]}</Badge>
                  <Badge variant={h.urgency === "crisis" || h.urgency === "high" ? "danger" : "muted"}>
                    {h.urgency}
                  </Badge>
                  <span className="ml-auto text-[11px] text-subtle">{timeAgo(h.createdAt)}</span>
                </div>
                <h3 className="mt-1 font-display font-semibold">{h.title}</h3>
                <p className="text-sm text-muted">{h.body}</p>
                <p className="mt-1 text-xs text-subtle">
                  {h.authorName} · {h.district}
                </p>
                <Button
                  size="sm"
                  className="mt-3"
                  variant="secondary"
                  onClick={async () => {
                    const res = await sendMessage({
                      data: {
                        toProfileId: h.authorId,
                        body: h.kind === "need" ? "Mogę pomóc. Napisz, co jest potrzebne." : "Dziękuję, chętnie skorzystam.",
                      },
                    });
                    navigate({ to: "/messages/$id", params: { id: res.threadId } });
                  }}
                >
                  Napisz
                </Button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
