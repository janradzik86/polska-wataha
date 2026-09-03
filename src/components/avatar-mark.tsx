import { cn } from "@/lib/utils";

export function AvatarMark({
  name,
  hue,
  size = "md",
}: {
  name: string;
  hue: number;
  size?: "sm" | "md" | "lg";
}) {
  const initials = name
    .split(" ")
    .slice(0, 2)
    .map((p) => p[0])
    .join("")
    .toUpperCase();
  const dim = size === "sm" ? "size-8 text-[10px]" : size === "lg" ? "size-14 text-lg" : "size-10 text-xs";
  return (
    <div
      className={cn(
        "grid shrink-0 place-items-center rounded-full font-display font-semibold text-bg",
        dim,
      )}
      style={{ background: `hsl(${(hue % 20) + 350} 62% 38%)` }}
      aria-hidden
    >
      {initials}
    </div>
  );
}
