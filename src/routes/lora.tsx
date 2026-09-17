import { useEffect, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { Guard } from "@/components/guard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  disconnectLora,
  loraState,
  onLoraChange,
  pairHeltec,
  sendLoraLine,
  webBluetoothOk,
} from "@/lib/comm/lora";

export const Route = createFileRoute("/lora")({ component: Page });

function Page() {
  return (
    <Guard>
      <LoraDesk />
    </Guard>
  );
}

function LoraDesk() {
  const [, setTick] = useState(0);
  const [body, setBody] = useState("WATAHA ping");
  const [pin, setPin] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const st = loraState();
  const bt = webBluetoothOk();

  useEffect(() => onLoraChange(() => setTick((n) => n + 1)), []);

  async function pair() {
    setErr(null);
    try {
      await pairHeltec(pin);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Nie udało się sparować");
    }
  }

  async function send() {
    setErr(null);
    if (!st.paired) {
      setErr("Najpierw PIN z OLED — radio nie nada bez parowania.");
      return;
    }
    try {
      await sendLoraLine(JSON.stringify({ v: 1, t: "tx", k: "msg", body, ts: Date.now() }));
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Błąd TX");
    }
  }

  const status = st.paired ? "up" : st.connected ? "PIN" : "ready";

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <p className="text-xs uppercase tracking-[0.2em] text-muted">V0.4 · SX1262 · EU868</p>
        <h1 className="font-display text-3xl font-semibold">Heltec WiFi LoRa 32 V4</h1>
        <p className="max-w-prose text-sm text-muted">
          Moduł z pudełka LoRa Dev-kits, pasmo HF 863–928 MHz. Telefon łączy się po Bluetooth LE
          (Nordic UART). Radio nadaje na 868.1 MHz, SF7, 14 dBm — zgodnie z EU. PIN z OLED jest
          weryfikowany na module. Bez PIN radio milczy.
        </p>
      </header>

      <div className="space-y-3 rounded-xl border border-border bg-surface p-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="font-display text-lg">Bramka</p>
            <p className="text-sm text-muted">
              {st.paired
                ? `${st.deviceName || "Heltec V4"} · PIN OK`
                : st.connected
                  ? "Połączono BLE — czekam na PIN"
                  : "Niepodłączona"}
            </p>
          </div>
          <Badge variant={st.paired ? "ok" : "warn"}>{status}</Badge>
        </div>
        {!bt ? (
          <p className="text-sm text-warn">
            Web Bluetooth działa w Chrome i Edge na HTTPS. Na telefonie użyj APK — tam BLE jest natywne.
          </p>
        ) : null}
        {err ? <p className="text-sm text-danger">{err}</p> : null}
        <Input
          inputMode="numeric"
          maxLength={6}
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, "").slice(0, 6))}
          placeholder="6-cyfrowy PIN z OLED"
        />
        <div className="flex flex-wrap gap-2">
          <Button onClick={pair} disabled={!bt || pin.length !== 6}>
            Sparuj Heltec V4
          </Button>
          {st.connected ? (
            <Button variant="secondary" onClick={disconnectLora}>
              Rozłącz
            </Button>
          ) : null}
        </div>
        <p className="text-xs text-subtle">
          Po wgraniu firmware na OLED: nazwa WATAHA-xxxx i PIN. 5 błędnych prób = blokada 5 min.
        </p>
      </div>

      <div className="space-y-3 rounded-xl border border-border bg-surface p-4">
        <h2 className="font-display text-lg">Nadaj ramkę</h2>
        <Input value={body} onChange={(e) => setBody(e.target.value)} placeholder="Treść" />
        <Button onClick={send} disabled={!st.paired} variant="secondary">
          TX przez SX1262
        </Button>
        <ul className="max-h-56 space-y-1 overflow-auto font-mono text-xs text-muted">
          {st.log.length === 0 ? <li>Brak ramek.</li> : null}
          {st.log.map((l, i) => (
            <li key={i}>{l}</li>
          ))}
        </ul>
      </div>

      <section className="space-y-2 text-sm text-muted">
        <h2 className="font-display text-xl text-fg">Wgrywanie firmware</h2>
        <ol className="list-decimal space-y-1 pl-5">
          <li>Arduino IDE — płyta Heltec WiFi LoRa 32 (V3), pinout V4 jest zgodny.</li>
          <li>Biblioteki: RadioLib, U8g2.</li>
          <li>
            Szkic:{" "}
            <a className="text-primary underline" href="/firmware/wataha_gateway.ino" download>
              wataha_gateway.ino
            </a>
          </li>
          <li>USB-C, wgraj, na OLED: nazwa WATAHA-xxxx i PIN.</li>
          <li>W aplikacji: Radio LoRa → Skanuj → PIN → wyślij test.</li>
        </ol>
      </section>
    </div>
  );
}
