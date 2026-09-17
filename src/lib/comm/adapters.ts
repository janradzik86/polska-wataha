import { loraState } from "@/lib/comm/lora";

export type AdapterHealth = "up" | "ready" | "hardware_missing" | "down";

export type AdapterStatus = {
  id: "internet" | "bluetooth" | "wifi_direct" | "lora";
  label: string;
  layer: string;
  health: AdapterHealth;
  detail: string;
  stage: string;
};

export function inspectAdapters(online: boolean): AdapterStatus[] {
  const lora = loraState();
  const loraUp = lora.paired && lora.connected;
  return [
    {
      id: "internet",
      label: "InternetAdapter",
      layer: "IP / HTTPS",
      health: online ? "up" : "down",
      detail: online
        ? "Kanał IP aktywny. Synchronizacja z backendem możliwa."
        : "Brak IP. Ruch trafia do kolejki store-and-forward.",
      stage: "V0.4 — działający",
    },
    {
      id: "bluetooth",
      label: "BluetoothAdapter",
      layer: "BLE 5 · UART",
      health: lora.connected ? "up" : "ready",
      detail: lora.connected
        ? `BLE do bramki ${lora.deviceName || "Heltec V4"}.`
        : "Skan BLE pod Heltec WiFi LoRa 32 V4 (nazwa WATAHA-xxxx).",
      stage: "V0.4 — łącze do radia",
    },
    {
      id: "wifi_direct",
      label: "WifiDirectAdapter",
      layer: "Wi-Fi P2P",
      health: "ready",
      detail: "Warstwa P2P gotowa. Ruch kryzysowy idzie LoRa + BLE, nie P2P.",
      stage: "V0.3 — przygotowany",
    },
    {
      id: "lora",
      label: "LoRaAdapter",
      layer: "SX1262 · EU868",
      health: loraUp ? "up" : "ready",
      detail: loraUp
        ? `Heltec V4 · PIN OK · ${lora.lastLine || "oczekiwanie na ramki"}`
        : "Podłącz Heltec WiFi LoRa 32 V4 (HF 863–928). Firmware Watahy, PIN na OLED.",
      stage: "V0.4 — sprzęt Heltec V4",
    },
  ];
}

export type MeshHop = { from: string; to: string; via: string[]; ok: boolean; reason?: string };

export function routeMesh(
  nodes: { id: string; status: string }[],
  from: string,
  to: string,
): MeshHop {
  const order = ["NODE_A", "NODE_B", "NODE_C", "NODE_D"];
  const down = new Set(nodes.filter((n) => n.status === "down").map((n) => n.id));
  const path = [...order];
  const i = path.indexOf(from);
  const j = path.indexOf(to);
  if (i < 0 || j < 0) return { from, to, via: [], ok: false, reason: "Nieznany węzeł" };
  const dir = i < j ? 1 : -1;
  const via: string[] = [];
  for (let k = i; k !== j; k += dir) {
    const hop = path[k];
    if (down.has(hop) && hop !== from) {
      const alt = path.filter((id) => !down.has(id));
      if (alt.includes(from) && alt.includes(to) && alt.length >= 2) {
        return {
          from,
          to,
          via: alt.filter((id) => id !== from && id !== to),
          ok: true,
          reason: `${hop} niedostępny — rerouting przez pozostałe węzły`,
        };
      }
      return { from, to, via, ok: false, reason: `Przerwany łańcuch przy ${hop}` };
    }
    if (hop !== from) via.push(hop);
  }
  return { from, to, via, ok: true };
}
