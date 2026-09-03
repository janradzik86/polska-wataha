import { useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Guard } from "@/components/guard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { createListing } from "@/lib/server/siatka";
import { CATEGORIES, KIND_LABEL, type ListingKind } from "@/lib/siatka";
import { isEffectivelyOnline, useSiatka } from "@/lib/store";

export const Route = createFileRoute("/listings/new")({ component: Page });

function Page() {
  return (
    <Guard>
      <Form />
    </Guard>
  );
}

function Form() {
  const navigate = useNavigate();
  const enqueue = useSiatka((s) => s.enqueue);
  const [kind, setKind] = useState<ListingKind>("offer");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [category, setCategory] = useState<string>(CATEGORIES[0]);
  const [district, setDistrict] = useState("Śródmieście");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const payload = { kind, title, body, category, district };
    try {
      if (!isEffectivelyOnline()) {
        enqueue({ action: "listing.create", payload: JSON.stringify(payload) });
        navigate({ to: "/status" });
        return;
      }
      const res = await createListing({ data: payload });
      navigate({ to: "/listings/$id", params: { id: res.id } });
    } catch (err) {
      enqueue({ action: "listing.create", payload: JSON.stringify(payload) });
      setError(err instanceof Error ? err.message : "Zapisano do kolejki offline");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="mx-auto max-w-lg space-y-4" onSubmit={onSubmit}>
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Nowe</p>
        <h1 className="font-display text-3xl font-semibold">Ogłoszenie</h1>
      </div>
      <div className="grid grid-cols-2 gap-2">
        {(Object.keys(KIND_LABEL) as ListingKind[]).map((k) => (
          <Button key={k} type="button" variant={kind === k ? "default" : "outline"} onClick={() => setKind(k)}>
            {KIND_LABEL[k]}
          </Button>
        ))}
      </div>
      <div className="space-y-1">
        <Label htmlFor="title">Tytuł</Label>
        <Input id="title" required minLength={3} value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="body">Opis</Label>
        <Textarea id="body" required minLength={3} value={body} onChange={(e) => setBody(e.target.value)} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="cat">Kategoria</Label>
        <select
          id="cat"
          className="flex h-11 w-full rounded-md border border-border bg-elevated px-3 text-sm"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        >
          {CATEGORIES.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </div>
      <div className="space-y-1">
        <Label htmlFor="dist">Dzielnica</Label>
        <Input id="dist" value={district} onChange={(e) => setDistrict(e.target.value)} />
      </div>
      {error ? <p className="text-sm text-warn">{error}</p> : null}
      <Button type="submit" className="w-full" disabled={busy}>
        Opublikuj
      </Button>
    </form>
  );
}
