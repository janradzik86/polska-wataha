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
      → LoRaAdapter            (warstwa, bez udawania radia)`}
          </pre>
        </section>
        <section className="space-y-2">
          <h2 className="font-display text-xl text-fg">Działa w V0.2</h2>
          <ul className="list-disc pl-5">
            <li>Konta, logowanie, konto demo</li>
            <li>Ogłoszenia, wyszukiwanie, oddawanie, wymiana</li>
            <li>Pomoc: zgłoszenie potrzeby i oferta</li>
            <li>Wiadomości, reputacja, odznaki</li>
            <li>Mapa (bez klucza Google), lokalizacja na żądanie</li>
            <li>Tryb kryzysowy, status sieci, kolejka offline</li>
            <li>Mesh Lab: NODE A–D, awaria B, rerouting</li>
            <li>Backend Postgres + REST /api/v1</li>
            <li>APK: SQLite, powiadomienia, adaptery, uprawnienia just-in-time</li>
          </ul>
        </section>
        <section className="space-y-2">
          <h2 className="font-display text-xl text-fg">Przygotowane na kolejne etapy</h2>
          <ul className="list-disc pl-5">
            <li>V0.3 — testy BLE / Wi-Fi Direct urządzenie–urządzenie</li>
            <li>V0.4 — fizyczny moduł LoRa (SX126x), bez udawania w telefonie</li>
            <li>V0.5 — mesh store-and-forward między węzłami</li>
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
