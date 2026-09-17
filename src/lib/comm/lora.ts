export const NUS_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
export const NUS_RX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
export const NUS_TX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

export type LoraLink = {
  deviceName: string;
  connected: boolean;
  paired: boolean;
  lastLine: string;
  log: string[];
};

type BtDevice = {
  gatt?: {
    connected: boolean;
    connect: () => Promise<BtServer>;
    disconnect: () => void;
  };
  name?: string;
};

type BtServer = {
  getPrimaryService: (u: string) => Promise<BtService>;
};

type BtService = {
  getCharacteristic: (u: string) => Promise<BtChar>;
};

type BtChar = {
  writeValue: (d: BufferSource) => Promise<void>;
  startNotifications: () => Promise<BtChar>;
  addEventListener: (type: string, fn: (ev: { target: { value?: DataView } }) => void) => void;
};

declare global {
  interface Navigator {
    bluetooth?: {
      requestDevice: (opts: unknown) => Promise<BtDevice>;
    };
  }
}

let device: BtDevice | null = null;
let rx: BtChar | null = null;
let log: string[] = [];
let listeners: Array<() => void> = [];
let paired = false;

export function loraState(): LoraLink {
  return {
    deviceName: device?.name ?? "",
    connected: Boolean(device?.gatt?.connected),
    paired,
    lastLine: log[0] ?? "",
    log: log.slice(0, 40),
  };
}

export function onLoraChange(fn: () => void) {
  listeners.push(fn);
  return () => {
    listeners = listeners.filter((x) => x !== fn);
  };
}

function bump(line?: string) {
  if (line) {
    log = [`${new Date().toLocaleTimeString("pl-PL")}  ${line}`, ...log].slice(0, 80);
  }
  listeners.forEach((f) => f());
}

export function webBluetoothOk() {
  return typeof navigator !== "undefined" && Boolean(navigator.bluetooth);
}

export async function pairHeltec(pin: string): Promise<void> {
  const clean = pin.replace(/\D/g, "");
  if (clean.length !== 6) throw new Error("PIN z OLED ma 6 cyfr.");
  if (!navigator.bluetooth) throw new Error("Web Bluetooth wymaga Chrome / Edge (HTTPS).");
  paired = false;
  const dev = await navigator.bluetooth.requestDevice({
    filters: [{ namePrefix: "WATAHA" }, { namePrefix: "Heltec" }, { services: [NUS_SERVICE] }],
    optionalServices: [NUS_SERVICE],
  });
  device = dev;
  const server = await dev.gatt!.connect();
  const svc = await server.getPrimaryService(NUS_SERVICE);
  const tx = await svc.getCharacteristic(NUS_TX);
  rx = await svc.getCharacteristic(NUS_RX);
  await tx.startNotifications();
  tx.addEventListener("characteristicvaluechanged", (ev) => {
    const v = ev.target.value;
    if (!v) return;
    const text = new TextDecoder().decode(v.buffer);
    const line = text.trim();
    try {
      const o = JSON.parse(line) as { t?: string; ok?: boolean; reason?: string };
      if (o.t === "paired") {
        paired = Boolean(o.ok);
        bump(paired ? "PIN zaakceptowany — LoRa gotowe." : `PIN odrzucony (${o.reason || "zły PIN"}).`);
        if (!paired) {
          try {
            device?.gatt?.disconnect();
          } catch {
            /* ignore */
          }
        }
        return;
      }
    } catch {
      /* raw line */
    }
    bump(line);
  });
  await sendLoraLine(JSON.stringify({ v: 1, t: "hello", from: "web" }));
  await sendLoraLine(JSON.stringify({ v: 1, t: "pair", pin: clean }));
  bump(`Wysłano PIN do ${dev.name ?? "Heltec V4"}`);
}

export async function sendLoraLine(line: string) {
  if (!rx) throw new Error("Brak łącza LoRa");
  const payload = line.endsWith("\n") ? line : `${line}\n`;
  await rx.writeValue(new TextEncoder().encode(payload));
  bump(`TX ${line}`);
}

export function disconnectLora() {
  try {
    device?.gatt?.disconnect();
  } catch {
    /* ignore */
  }
  device = null;
  rx = null;
  paired = false;
  bump("Rozłączono");
}
