# Nawigacja offline — Polska Wataha

Rdzeń nawigacji ma działać local-first i nie może udawać funkcji, których jeszcze nie podłączono.

Gotowe w rdzeniu:
- modele punktów, tras i manewrów,
- WALK / BIKE / CAR,
- rejestr paczek offline,
- sprawdzanie pokrycia,
- kontrakt lokalnego wyszukiwania,
- kontrakt lokalnego routera,
- aktywna nawigacja, odległość do trasy/celu, reroute i wykrycie dotarcia.

Format `WATAHA_MAP_PACK_V1`:
- manifest.json,
- map.pmtiles,
- search.db,
- routing/,
- signature / sha256.

Stan integracji:
- renderer mapy web pozostaje bez masowego pobierania publicznych kafelków OSM,
- natywny BRouter nie jest jeszcze podłączony do realnych grafów,
- `BrouterRoutingProvider.available` pozostaje `false`,
- placeholder routera nie zwraca wymyślonej trasy,
- brak opublikowanych realnych paczek PMTiles województw.

Do spięcia z Androidem:
1. MapLibre Native,
2. SQLite FTS,
3. BRouter lub inny lokalny router,
4. Android LocationManager/Fused Location,
5. downloader i weryfikacja paczek.

Tryb kryzysowy:
- bez nowych pobrań,
- bez API,
- używa ostatniej zweryfikowanej lokalnej paczki,
- GPS/GNSS + routing lokalny, jeśli graf jest zainstalowany.

Nie używać `tile.openstreetmap.org` do masowego pobierania map offline.
