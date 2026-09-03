import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { AvatarMark } from "@/components/avatar-mark";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { getPerson, sendMessage } from "@/lib/server/siatka";

export const Route = createFileRoute("/people/$id")({ component: Page });

function Page() {
  const { id } = Route.useParams();
  return (
    <Guard>
      <Person id={id} />
    </Guard>
  );
}

function Person({ id }: { id: string }) {
  const navigate = useNavigate();
  const q = useQuery({ queryKey: ["person", id], queryFn: () => getPerson({ data: { id } }) });
  const p = q.data;
  if (!p) return <p className="text-sm text-muted">Ładowanie profilu…</p>;
  return (
    <div className="mx-auto max-w-lg space-y-5">
      <div className="flex items-center gap-4">
        <AvatarMark name={p.displayName} hue={p.avatarHue} size="lg" />
        <div>
          <h1 className="font-display text-3xl font-semibold">{p.displayName}</h1>
          <p className="text-sm text-muted">
            {p.district} · reputacja {p.reputation}
          </p>
        </div>
      </div>
      <p className="text-sm text-muted">{p.bio || "Brak opisu."}</p>
      <div className="flex flex-wrap gap-2">
        {p.badges.map((b) => (
          <Badge key={b.id} variant="default">
            {b.title}
          </Badge>
        ))}
      </div>
      <Button
        className="w-full"
        onClick={async () => {
          const res = await sendMessage({ data: { toProfileId: p.id, body: "Cześć! Piszę z Siatki." } });
          navigate({ to: "/messages/$id", params: { id: res.threadId } });
        }}
      >
        Wyślij wiadomość
      </Button>
    </div>
  );
}
