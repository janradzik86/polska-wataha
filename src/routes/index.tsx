import { useMemo, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Plus, Search } from "lucide-react";
import { Guard } from "@/components/guard";
import { ListingCard } from "@/components/listing-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { bootstrap, listListings } from "@/lib/server/siatka";
import { CATEGORIES, type ListingKind } from "@/lib/siatka";
import { OrzelBialy } from "@/components/brand";

export const Route = createFileRoute("/")({ component: Home });

function Home() {
  return (
    <Guard>
      <Feed />
    </Guard>
  );
}

function Feed() {
  const [q, setQ] = useState("");
  const [kind, setKind] = useState<ListingKind | "all">("all");
  const [category, setCategory] = useState<string>("all");
  useQuery({ queryKey: ["boot"], queryFn: () => bootstrap() });
  const listings = useQuery({
    queryKey: ["listings", q, kind, category],
    queryFn: () =>
      listListings({
        data: {
          q: q || undefined,
          kind: kind === "all" ? undefined : kind,
          category: category === "all" ? undefined : category,
        },
      }),
  });
  const items = listings.data ?? [];
  const kinds = useMemo(
    () =>
      [
        ["all", "Wszystkie"],
        ["offer", "Oferuję"],
        ["want", "Szukam"],
        ["giveaway", "Oddam"],
        ["exchange", "Wymiana"],
      ] as const,
    [],
  );

  return (
    <div className="space-y-5">
      <div className="flex items-end justify-between gap-3">
        <div>
          <p className="flex items-center gap-1.5 text-xs uppercase tracking-[0.2em] text-primary">
            <OrzelBialy className="h-3.5 w-3" />
            Czarne Wilki Prawdy
          </p>
          <h1 className="font-display text-3xl font-semibold">Ogłoszenia</h1>
        </div>
        <Button asChild>
          <Link to="/listings/new">
            <Plus className="size-4" />
            Nowe
          </Link>
        </Button>
      </div>
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-subtle" />
        <Input
          className="pl-9"
          placeholder="Szukaj: wiertarka, Mokotów, rower…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>
      <Tabs value={kind} onValueChange={(v) => setKind(v as ListingKind | "all")}>
        <TabsList className="w-full justify-start overflow-x-auto">
          {kinds.map(([id, label]) => (
            <TabsTrigger key={id} value={id}>
              {label}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>
      <div className="flex gap-2 overflow-x-auto pb-1">
        <Button size="sm" variant={category === "all" ? "default" : "outline"} onClick={() => setCategory("all")}>
          Kategorie
        </Button>
        {CATEGORIES.map((c) => (
          <Button key={c} size="sm" variant={category === c ? "default" : "outline"} onClick={() => setCategory(c)}>
            {c}
          </Button>
        ))}
      </div>
      <div className="grid gap-3">
        {items.map((item) => (
          <ListingCard key={item.id} item={item} />
        ))}
        {!listings.isLoading && items.length === 0 ? (
          <p className="rounded-xl border border-border bg-surface p-6 text-sm text-muted">
            Brak ogłoszeń. Dodaj pierwsze do watahy.
          </p>
        ) : null}
      </div>
    </div>
  );
}
