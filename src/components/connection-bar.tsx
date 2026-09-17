import { useEffect, useState } from "react";
import { Radio, WifiOff } from "lucide-react";
import { useSiatka } from "@/lib/store";
import { inspectAdapters } from "@/lib/comm/adapters";
import { onLoraChange } from "@/lib/comm/lora";

export function ConnectionBar() {
  const online = useSiatka((s) => s.online);
  const simulate = useSiatka((s) => s.simulateOffline);
  const setOnline = useSiatka((s) => s.setOnline);
  const [, setTick] = useState(0);
  const effective = online && !simulate;
  const adapters = inspectAdapters(effective);
  const net = adapters[0];
  const lora = adapters[3];

  useEffect(() => {
    const on = () => setOnline(true);
    const off = () => setOnline(false);
    window.addEventListener("online", on);
    window.addEventListener("offline", off);
    setOnline(navigator.onLine);
    const offLora = onLoraChange(() => setTick((n) => n + 1));
    return () => {
      window.removeEventListener("online", on);
      window.removeEventListener("offline", off);
      offLora();
    };
  }, [setOnline]);

  const loraUp = lora.health === "up";
  return (
    <div
      className={
        effective || loraUp
          ? "flex items-center gap-2 border-b border-border bg-surface px-4 py-1.5 text-[11px] text-muted"
          : "flex items-center gap-2 border-b border-border bg-elevated px-4 py-1.5 text-[11px] text-warn"
      }
    >
      {effective || loraUp ? <Radio className="size-3.5" /> : <WifiOff className="size-3.5" />}
      <span className="font-medium tracking-wide uppercase">
        {loraUp && effective ? "IP + LoRa" : loraUp ? "LoRa EU868" : effective ? "Sieć IP" : "Tryb offline"}
      </span>
      <span className="truncate">{loraUp ? lora.detail : net.detail}</span>
    </div>
  );
}