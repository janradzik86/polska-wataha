import { createFileRoute } from "@tanstack/react-router";
import { Guard } from "@/components/guard";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/download")({ component: Page });

function Page() {
  return (
    <Guard>
      <div className="mx-auto max-w-lg space-y-4">
        <p className="text-xs uppercase tracking-[0.2em] text-muted">Android</p>
        <h1 className="font-display text-3xl font-semibold">APK do instalacji</h1>
        <p className="text-sm text-muted">
          Gotowy plik instalacyjny. Nie wymaga Android Studio. Na telefonie: pobierz, otwórz, zezwól na
          instalację z tej przeglądarki, uruchom Polską Watahę.
        </p>
        <ol className="list-decimal space-y-2 pl-5 text-sm text-muted">
          <li>Pobierz <span className="text-fg">app-release.apk</span>.</li>
          <li>Otwórz plik w powiadomieniu lub w folderze Pobrane.</li>
          <li>Zezwól na instalację z tego źródła, jeśli system o to poprosi.</li>
          <li>Otwórz Polską Watahę. Wejdź kontem demo albo załóż własne.</li>
          <li>Aplikacja działa offline — baza SQLite jest w telefonie.</li>
        </ol>
        <Button asChild className="w-full">
          <a href="/downloads/app-release.apk" download>
            Pobierz app-release.apk
          </a>
        </Button>
        <p className="text-xs text-subtle">
          Pakiet: app.siatka · min. Android 8.0 · podpis testowy (sideload). Źródła w katalogu android/.
        </p>
      </div>
    </Guard>
  );
}
