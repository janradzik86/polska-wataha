import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { AvatarMark } from "@/components/avatar-mark";
import { Badge } from "@/components/ui/badge";
import { listPeople } from "@/lib/server/siatka";

export const Route = createFileRoute("/people")({ component: Page });

function Page() {
  return (
    <Guard>
      <People />
    </Guard>
  );
}

function People() {
  const q = useQuery({ queryKey: ["people"], queryFn: () => listPeople() });
  return (
    <div className="space-y-4">
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Sąsiedzi</p>
        <h1 className="font-display text-3xl font-semibold">Ludzie</h1>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {(q.data ?? []).map((p) => (
          <Link
            key={p.id}
            to="/people/$id"
            params={{ id: p.id }}
            className="rounded-xl border border-border bg-surface p-4"
          >
            <div className="flex items-start gap-3">
              <AvatarMark name={p.displayName} hue={p.avatarHue} />
              <div>
                <p className="font-display font-semibold">{p.displayName}</p>
                <p className="text-xs text-muted">
                  {p.district} · reputacja {p.reputation}
                </p>
                <div className="mt-2 flex flex-wrap gap-1">
                  {p.badges.slice(0, 3).map((b) => (
                    <Badge key={b.id} variant="muted">
                      {b.title}
                    </Badge>
                  ))}
                </div>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
