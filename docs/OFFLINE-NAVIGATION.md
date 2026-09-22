# WATAHA OFFLINE NAVIGATION CORE

Rdzeń nawigacji dla Polskiej Watahy.

Gotowe:
- modele mapy, punktów, tras i manewrów,
- TravelMode WALK/BIKE/CAR,
- rejestr paczek offline,
- sprawdzanie pokrycia,
- kontrakt lokalnego wyszukiwania,
- kontrakt lokalnego routera,
- silnik aktywnej nawigacji,
- dystans do trasy/celu/manewru,
- wykrywanie zejścia/zjazdu z trasy,
- sygnał przeliczenia,
- wykrycie dotarcia do celu.

Do spięcia z Androidem:
1. MapLibre Native jako renderer PMTiles.
2. SQLite FTS jako indeks adresów/POI.
3. BRouter lub inny router offline implementujący OfflineRoutingProvider.
4. Android LocationManager/Fused Location implementujący LocationProvider.
5. Downloader paczek województw w normalnym trybie.

Format WATAHA_MAP_PACK_V1:
- manifest.json
- map.pmtiles
- search.db
- routing/
- signature / sha256

Tryb kryzysowy:
- bez pobrań,
- bez API,
- ostatnia zweryfikowana paczka,
- GPS + routing lokalny.

Nie używać publicznego tile.openstreetmap.org do masowego pobierania map offline.
