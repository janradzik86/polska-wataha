import { Link } from "@tanstack/react-router";
import { KIND_LABEL, type Listing } from "@/lib/siatka";
import { timeAgo } from "@/lib/utils";
import { AvatarMark } from "@/components/avatar-mark";
import { Badge } from "@/components/ui/badge";

export function ListingCard({ item }: { item: Listing }) {
  return (
    <Link
      to="/listings/$id"
      params={{ id: item.id }}
      className="block rounded-xl border border-border border-l-4 border-l-primary bg-surface p-4 transition-opacity hover:opacity-90"
    >
      <div className="flex items-start gap-3">
        <AvatarMark name={item.authorName} hue={item.authorHue} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="solid">{KIND_LABEL[item.kind]}</Badge>
            <span className="text-[11px] uppercase tracking-wider text-muted">{item.category}</span>
            <span className="ml-auto text-[11px] text-subtle">{timeAgo(item.createdAt)}</span>
          </div>
          <h3 className="mt-1 font-display text-base font-semibold leading-snug">{item.title}</h3>
          <p className="mt-1 line-clamp-2 text-sm text-muted">{item.body}</p>
          <p className="mt-2 text-xs text-subtle">
            {item.authorName} · {item.district}
          </p>
        </div>
      </div>
    </Link>
  );
}
