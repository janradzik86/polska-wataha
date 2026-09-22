export type RegionPack = { id: string; name: string };

export const REGIONS: RegionPack[] = [
  { id: "pl", name: "Polska" },
  { id: "dolnoslaskie", name: "Dolnośląskie" },
  { id: "kujawsko-pomorskie", name: "Kujawsko-Pomorskie" },
  { id: "lubelskie", name: "Lubelskie" },
  { id: "lubuskie", name: "Lubuskie" },
  { id: "lodzkie", name: "Łódzkie" },
  { id: "malopolskie", name: "Małopolskie" },
  { id: "mazowieckie", name: "Mazowieckie" },
  { id: "opolskie", name: "Opolskie" },
  { id: "podkarpackie", name: "Podkarpackie" },
  { id: "podlaskie", name: "Podlaskie" },
  { id: "pomorskie", name: "Pomorskie" },
  { id: "slaskie", name: "Śląskie" },
  { id: "swietokrzyskie", name: "Świętokrzyskie" },
  { id: "warminsko-mazurskie", name: "Warmińsko-Mazurskie" },
  { id: "wielkopolskie", name: "Wielkopolskie" },
  { id: "zachodniopomorskie", name: "Zachodniopomorskie" },
];

export const MAP_PACK_FORMAT = "WATAHA_MAP_PACK_V1";

/** Kryzys i brak sieci blokują pobieranie. Brak opublikowanego PMTiles też. */
export function requestPackDownload(input: { crisis: boolean; online: boolean }): { ok: false; reason: string } {
  if (input.crisis) {
    return { ok: false, reason: "Tryb kryzysowy. Nie pobieram map. Zostaje to, co już jest na telefonie." };
  }
  if (!input.online) return { ok: false, reason: "Brak internetu. Nie pobieram paczki." };
  return {
    ok: false,
    reason: "Nie ma jeszcze opublikowanego źródła PMTiles. Nie pobieram kafelków z tile.openstreetmap.org.",
  };
}

export class BrouterRoutingProvider {
  readonly id = "brouter";
  readonly available = false;
  plan(): null {
    return null;
  }
}

/** Adapter jest, silnik grafu w tej aplikacji jeszcze nie liczy trasy. */
export class PackGraphRoutingProvider {
  readonly id = "pack-graph";
  readonly available = false;
  plan(): null {
    return null;
  }
}
