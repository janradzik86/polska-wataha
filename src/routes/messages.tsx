import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { AvatarMark } from "@/components/avatar-mark";
import { listThreads } from "@/lib/server/siatka";
import { timeAgo } from "@/lib/utils";

export const Route = createFileRoute("/messages")({ component: Page });

function Page() {
  return (
    <Guard>
      <Inbox />
    </Guard>
  );
}

function Inbox() {
  const q = useQuery({ queryKey: ["threads"], queryFn: () => listThreads() });
  const items = q.data ?? [];
  return (
    <div className="space-y-4">
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Skrzynka</p>
        <h1 className="font-display text-3xl font-semibold">Wiadomości</h1>
      </div>
      {items.length === 0 && !q.isLoading ? (
        <p className="text-sm text-muted">Brak rozmów. Otwórz ogłoszenie albo profil sąsiada.</p>
      ) : null}
      <div className="grid gap-2">
        {items.map((t) => (
          <Link
            key={t.id}
            to="/messages/$id"
            params={{ id: t.id }}
            className="flex items-center gap-3 rounded-xl border border-border bg-surface p-3"
          >
            <AvatarMark name={t.peerName} hue={t.peerHue} />
            <div className="min-w-0 flex-1">
              <div className="flex justify-between gap-2">
                <p className="font-medium">{t.peerName}</p>
                <span className="text-[11px] text-subtle">{timeAgo(t.lastAt)}</span>
              </div>
              <p className="truncate text-sm text-muted">{t.lastBody || t.title}</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
