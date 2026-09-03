import { useEffect } from "react";
import { Radio, WifiOff } from "lucide-react";
import { useSiatka } from "@/lib/store";
import { inspectAdapters } from "@/lib/comm/adapters";

export function ConnectionBar() {
  const online = useSiatka((s) => s.online);
  const simulate = useSiatka((s) => s.simulateOffline);
  const setOnline = useSiatka((s) => s.setOnline);
  const effective = online && !simulate;
  const net = inspectAdapters(effective)[0];

  useEffect(() => {
    const on = () => setOnline(true);
    const off = () => setOnline(false);
    window.addEventListener("online", on);
    window.addEventListener("offline", off);
    setOnline(navigator.onLine);
    return () => {
      window.removeEventListener("online", on);
      window.removeEventListener("offline", off);
    };
  }, [setOnline]);

  return (
    <div
      className={
        effective
          ? "flex items-center gap-2 border-b border-border bg-surface px-4 py-1.5 text-[11px] text-muted"
          : "flex items-center gap-2 border-b border-border bg-elevated px-4 py-1.5 text-[11px] text-warn"
      }
    >
      {effective ? <Radio className="size-3.5" /> : <WifiOff className="size-3.5" />}
      <span className="font-medium tracking-wide uppercase">
        {effective ? "Sieć IP" : "Tryb offline"}
      </span>
      <span className="truncate">{net.detail}</span>
    </div>
  );
}
