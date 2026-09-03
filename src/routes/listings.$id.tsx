import { useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Guard } from "@/components/guard";
import { AvatarMark } from "@/components/avatar-mark";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { createExchange, getListing, sendMessage } from "@/lib/server/siatka";
import { KIND_LABEL } from "@/lib/siatka";
import { timeAgo } from "@/lib/utils";

export const Route = createFileRoute("/listings/$id")({ component: Page });

function Page() {
  const { id } = Route.useParams();
  return (
    <Guard>
      <Detail id={id} />
    </Guard>
  );
}

function Detail({ id }: { id: string }) {
  const navigate = useNavigate();
  const q = useQuery({ queryKey: ["listing", id], queryFn: () => getListing({ data: { id } }) });
  const [msg, setMsg] = useState("Cześć, jestem zainteresowany/a.");
  const [offer, setOffer] = useState("Mogę dać w zamian narzędzia / przysługę.");
  const [busy, setBusy] = useState(false);
  const item = q.data;

  if (q.isLoading) return <p className="text-sm text-muted">Wczytywanie…</p>;
  if (!item) return <p>Nie znaleziono ogłoszenia.</p>;
  const listing = item;

  async function message() {
    setBusy(true);
    try {
      const res = await sendMessage({ data: { toProfileId: listing.authorId, listingId: listing.id, body: msg } });
      navigate({ to: "/messages/$id", params: { id: res.threadId } });
    } finally {
      setBusy(false);
    }
  }

  async function exchange() {
    setBusy(true);
    try {
      const res = await createExchange({ data: { listingId: listing.id, offerText: offer } });
      navigate({ to: "/messages/$id", params: { id: res.threadId } });
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="mx-auto max-w-lg space-y-5">
      <Badge>{KIND_LABEL[listing.kind]}</Badge>
      <h1 className="font-display text-3xl font-semibold">{listing.title}</h1>
      <p className="text-sm text-muted">{listing.body}</p>
      <div className="flex items-center gap-3 rounded-xl border border-border bg-surface p-3">
        <AvatarMark name={listing.authorName} hue={listing.authorHue} />
        <div>
          <Link to="/people/$id" params={{ id: listing.authorId }} className="font-medium hover:underline">
            {listing.authorName}
          </Link>
          <p className="text-xs text-muted">
            {listing.district} · {listing.category} · {timeAgo(listing.createdAt)}
          </p>
        </div>
      </div>
      <div className="space-y-2">
        <p className="text-xs uppercase tracking-wider text-muted">Wiadomość</p>
        <Textarea value={msg} onChange={(e) => setMsg(e.target.value)} />
        <Button className="w-full" disabled={busy} onClick={message}>
          Wyślij wiadomość
        </Button>
      </div>
      <div className="space-y-2">
        <p className="text-xs uppercase tracking-wider text-muted">Propozycja wymiany</p>
        <Textarea value={offer} onChange={(e) => setOffer(e.target.value)} />
        <Button className="w-full" variant="secondary" disabled={busy} onClick={exchange}>
          Zaproponuj wymianę
        </Button>
      </div>
    </article>
  );
}
