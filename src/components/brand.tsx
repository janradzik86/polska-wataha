import { cn } from "@/lib/utils";
import { APP_NAME, APP_TAGLINE } from "@/lib/siatka";

export function FlagStripe({ className }: { className?: string }) {
  return <div className={cn("flag-stripe shrink-0", className)} aria-hidden />;
}

export function Kotwica({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 40" className={cn("fill-current", className)} aria-hidden>
      <path d="M14 2h4v6h8v4h-8v10.2c3.4-.8 6.7-1.2 10-1.2v4c-3.3 0-6.6.5-10 1.4V38h-4v-11.6C10.6 25.5 7.3 25 4 25v-4c3.3 0 6.6.4 10 1.2V12H6V8h8V2z" />
      <path d="M2 29h6l4 8H8l-2-4-2 4H0l4-8zm18 0h6l4 8h-4l-2-4-2 4h-4l4-8z" />
    </svg>
  );
}

export function OrzelBialy({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 120 140" className={cn("fill-current", className)} aria-hidden>
      <path d="M44 18 48 6l8 8 4-12 4 12 8-8 4 12-2 6H46z" />
      <path d="M54 28c0-6 4-10 10-12 4 0 8 4 10 10 8 2 14 8 16 14h-8c-2-6-6-8-10-8-2 4-6 6-10 6s-8-2-10-6c-4 0-8 2-10 8h-8c2-6 8-12 16-14z" />
      <path d="M18 56c12-8 24-10 36-8v12C42 60 30 64 20 74 12 82 8 94 8 106c8-10 16-20 22-26 4 14 10 24 16 32H34l-8 16h16l6-12h24l6 12h16l-8-16H74c6-8 12-18 16-32 6 6 14 16 22 26 0-12-4-24-12-32-10-10-22-14-34-14V48c12-2 24 0 36 8l8-10C90 34 74 30 60 32 46 30 30 34 10 46z" />
      <path d="M44 114h8l-6 18H34zm24 0h8l12 18H76z" />
      <circle cx="72" cy="34" r="2.2" className="fill-primary" />
    </svg>
  );
}

export function WolfMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={cn("block", className)} aria-hidden>
      <rect width="64" height="32" fill="#f7f5f2" />
      <rect y="32" width="64" height="32" fill="#c8102e" />
      <path
        fill="#0a0a0a"
        d="M12 22 L20 8 L26 18 L32 10 L38 18 L44 8 L52 22 L48 28 L54 36 L46 34 L50 48 L32 56 L14 48 L18 34 L10 36 L16 28 Z"
      />
      <path fill="#f7f5f2" d="M24 34 L32 30 L40 34 L32 42 Z" />
      <circle cx="24.5" cy="28" r="2" fill="#f7f5f2" />
      <circle cx="39.5" cy="28" r="2" fill="#f7f5f2" />
      <circle cx="24.5" cy="28" r="0.8" fill="#c8102e" />
      <circle cx="39.5" cy="28" r="0.8" fill="#c8102e" />
    </svg>
  );
}

export function BrandLockup({
  size = "md",
  stacked = false,
  compact = false,
}: {
  size?: "sm" | "md" | "lg";
  stacked?: boolean;
  compact?: boolean;
}) {
  const mark = size === "lg" ? "size-16" : size === "sm" ? "size-8" : "size-10";
  const title =
    size === "lg" ? "text-5xl sm:text-6xl" : size === "sm" ? "text-lg" : "text-xl";
  return (
    <div className={cn("flex items-center gap-3", stacked && "flex-col items-start gap-4")}>
      <img
        src="/brand/wataha-icon.jpg"
        alt=""
        className={cn(
          "shrink-0 rounded-md object-cover ring-1 ring-border",
          mark,
          size === "lg" && "rounded-lg ring-2 ring-primary",
        )}
      />
      <div>
        <p className={cn("font-display font-medium uppercase leading-none tracking-[0.08em] text-fg", title)}>
          {APP_NAME}
        </p>
        {compact ? null : (
          <p
            className={cn(
              "mt-1.5 flex items-center gap-1.5 font-display uppercase tracking-[0.18em] text-primary",
              size === "lg" ? "text-sm" : "text-[10px]",
            )}
          >
            <OrzelBialy className={size === "lg" ? "h-4 w-3.5" : "h-3 w-2.5"} />
            {APP_TAGLINE}
          </p>
        )}
      </div>
    </div>
  );
}

export function PatriotPanel({ className }: { className?: string }) {
  return (
    <div className={cn("relative overflow-hidden bg-primary", className)}>
      <div className="absolute inset-x-0 top-0 h-1/2 bg-flag" />
      <img
        src="/brand/wataha-banner.jpg"
        alt=""
        className="absolute inset-0 h-full w-full object-cover mix-blend-multiply opacity-80"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-flag/70 via-transparent to-primary/80" />
      <div className="relative z-10 flex h-full min-h-[22rem] flex-col justify-between p-8">
        <div className="flex items-center gap-3 text-bg">
          <OrzelBialy className="h-10 w-8" />
          <Kotwica className="h-8 w-6" />
        </div>
        <div>
          <p className="font-display text-5xl font-medium uppercase leading-none tracking-[0.06em] text-fg drop-shadow">
            Polska Wataha
          </p>
          <p className="mt-3 flex items-center gap-2 font-display text-sm uppercase tracking-[0.22em] text-flag">
            <Kotwica className="h-3.5 w-3" />
            Czarne Wilki Prawdy
          </p>
        </div>
      </div>
    </div>
  );
}
