import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { bootstrap, getMessages, sendMessage } from "@/lib/server/siatka";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/messages/$id")({ component: Page });

function Page() {
  const { id } = Route.useParams();
  return (
    <Guard>
      <Thread id={id} />
    </Guard>
  );
}

function Thread({ id }: { id: string }) {
  const qc = useQueryClient();
  const me = useQuery({ queryKey: ["boot"], queryFn: () => bootstrap() });
  const q = useQuery({ queryKey: ["msgs", id], queryFn: () => getMessages({ data: { threadId: id } }) });
  const [body, setBody] = useState("");
  const myId = me.data?.profileId;

  async function send() {
    if (!body.trim()) return;
    await sendMessage({ data: { threadId: id, body } });
    setBody("");
    await qc.invalidateQueries({ queryKey: ["msgs", id] });
  }

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-4">
      <h1 className="font-display text-2xl font-semibold">Rozmowa</h1>
      <div className="space-y-2">
        {(q.data ?? []).map((m) => (
          <div
            key={m.id}
            className={cn(
              "max-w-[85%] rounded-lg px-3 py-2 text-sm",
              m.senderId === myId ? "ml-auto bg-primary text-primary-fg" : "bg-elevated",
            )}
          >
            <p className="text-[10px] uppercase tracking-wider opacity-70">{m.senderName}</p>
            <p>{m.body}</p>
          </div>
        ))}
      </div>
      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          void send();
        }}
      >
        <Input value={body} onChange={(e) => setBody(e.target.value)} placeholder="Napisz…" />
        <Button type="submit">Wyślij</Button>
      </form>
    </div>
  );
}
