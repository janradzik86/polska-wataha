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
  return [
    {
      id: "internet",
      label: "InternetAdapter",
      layer: "IP / HTTPS",
      health: online ? "up" : "down",
      detail: online
        ? "Kanał IP aktywny. Synchronizacja z backendem możliwa."
        : "Brak IP. Ruch trafia do kolejki store-and-forward.",
      stage: "V0.1 — działający",
    },
    {
      id: "bluetooth",
      label: "BluetoothAdapter",
      layer: "BLE / Classic",
      health: "ready",
      detail: "Skanowanie i ramki testowe gotowe. Wymaga uprawnienia Bluetooth w momencie użycia.",
      stage: "V0.3 — przygotowany do testów",
    },
    {
      id: "wifi_direct",
      label: "WifiDirectAdapter",
      layer: "Wi-Fi P2P",
      health: "ready",
      detail: "Negocjacja grupy P2P zaimplementowana jako warstwa. Czeka na testy urządzenie–urządzenie.",
      stage: "V0.3 — przygotowany do testów",
    },
    {
      id: "lora",
      label: "LoRaAdapter",
      layer: "LoRa / SX126x",
      health: "hardware_missing",
      detail: "Brak modułu LoRa w tym telefonie. Adapter nie udaje transmisji radiowej — czeka na sprzęt (V0.4).",
      stage: "V0.4 — warstwa pod przyszły sprzęt",
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
