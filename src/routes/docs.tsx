import { createFileRoute } from "@tanstack/react-router";
import { Guard } from "@/components/guard";

export const Route = createFileRoute("/docs")({ component: Page });

function Page() {
  return (
    <Guard>
      <article className="mx-auto max-w-2xl space-y-6 text-sm leading-relaxed text-muted">
        <div>
          <p className="text-xs uppercase tracking-[0.2em]">Dokumentacja</p>
          <h1 className="font-display text-3xl font-medium uppercase tracking-wide text-fg">Architektura Watahy</h1>
        </div>
        <section className="space-y-2">
          <h2 className="font-display text-xl text-fg">Warstwy komunikacji</h2>
          <pre className="overflow-x-auto rounded-lg bg-elevated p-4 font-mono text-[11px] text-fg">
{`APPLICATION
  → COMMUNICATION SERVICE
    → COMMUNICATION ADAPTER
      → InternetAdapter        (działający)
      → BluetoothAdapter       (gotowy do testów)
      → WifiDirectAdapter      (gotowy do testów)
      → LoRaAdapter            (Heltec WiFi LoRa 32 V4, EU868)`}
          </pre>
        </section>
        <section className="space-y-2">
          <h2 className="font-display text-xl text-fg">Działa w V0.4</h2>
          <ul className="list-disc pl-5">
            <li>Konta z prawdziwym logowaniem (email/hasło, Google, X)</li>
            <li>Hasła PBKDF2 w APK, blokada po 5 błędach</li>
            <li>Ogłoszenia, pomoc, wiadomości, reputacja, mapa, kryzys</li>
            <li>Heltec WiFi LoRa 32 V4: BLE UART + SX1262 EU868, PIN weryfikowany na module (5 błędów = 5 min blokady)</li>
            <li>Store-and-forward gdy radio lub IP milczy</li>
            <li>Mesh A–D, NODE D = bramka LoRa</li>
            <li>APK v0.4.0 do instalacji na telefonie</li>
          </ul>
        </section>
        <section className="space-y-2">
          <h2 className="font-display text-xl text-fg">Przygotowane na kolejne etapy</h2>
          <ul className="list-disc pl-5">
            <li>V0.5 — mesh store-and-forward między wieloma Heltecami</li>
            <li>V1.0 — pełna sieć autonomicznych węzłów</li>
          </ul>
        </section>
        <section className="space-y-2">
          <h2 className="font-display text-xl text-fg">Dane testowe</h2>
          <p>
            Konto demo: demo@siatka.app / siatka-demo-2026. Sąsiedzi: Marek, Ewa, Piotr, Lena, Jan.
            Ogłoszenia, pomoc, punkty wody/żywności, węzły A–D, rozmowa z Markiem.
          </p>
        </section>
      </article>
    </Guard>
  );
}
