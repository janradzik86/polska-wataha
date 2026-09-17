import { Link, createFileRoute } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { BrandLockup, FlagStripe, OrzelBialy } from "@/components/brand";

export const Route = createFileRoute("/download")({ component: Page });

function Page() {
  return (
    <main className="min-h-dvh bg-bg text-fg">
      <FlagStripe />
      <div className="mx-auto max-w-lg space-y-5 px-6 py-10">
        <div className="flex items-center gap-3 text-primary">
          <OrzelBialy className="h-8 w-7" />
          <BrandLockup size="sm" />
        </div>
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Android · V0.4</p>
        <h1 className="font-display text-3xl font-semibold">APK do instalacji</h1>
        <p className="text-sm text-muted">
          Gotowy plik instalacyjny. Nie wymaga Android Studio. Na telefonie: pobierz, otwórz, zezwól na
          instalację z tej przeglądarki, uruchom Polską Watahę.
        </p>
        <ol className="list-decimal space-y-2 pl-5 text-sm text-muted">
          <li>
            Pobierz <span className="text-fg">app-release.apk</span>.
          </li>
          <li>Otwórz plik w powiadomieniu lub w folderze Pobrane.</li>
          <li>Zezwól na instalację z tego źródła, jeśli system o to poprosi.</li>
          <li>Otwórz Polską Watahę i załóż własne konto (hasło min. 8 znaków).</li>
          <li>Wgraj firmware na Heltec V4 USB-C (szkic poniżej).</li>
          <li>Profil → Radio LoRa: skan BLE, PIN z OLED, test ramki.</li>
          <li>Aplikacja działa offline — SQLite w telefonie, LoRa bez internetu.</li>
        </ol>
        <Button asChild className="w-full">
          <a href="/downloads/app-release.apk" download>
            Pobierz app-release.apk
          </a>
        </Button>
        <Button asChild variant="secondary" className="w-full">
          <a href="/firmware/wataha_gateway.ino" download>
            Pobierz firmware Heltec V4
          </a>
        </Button>
        <p className="text-xs text-subtle">
          Pakiet: app.siatka · v0.4.0 · min. Android 8.0 · podpis sideload. PIN z OLED jest
          sprawdzany na module — bez niego radio nie nada.
        </p>
        <p className="text-sm text-muted">
          <Link to="/login" className="text-primary underline">
            Wróć do logowania
          </Link>
        </p>
      </div>
    </main>
  );
}
