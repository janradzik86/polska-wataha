# Nawigacja offline — Polska Wataha

Renderer mapy w aplikacji web zostaje. Nie podmieniam go na pobieranie kafelków OSM.

`BrouterRoutingProvider.available` jest false. Brak natywnego BRoutera i brak plików PMTiles województw.
`plan()` zwraca null. Trasa do dziecka nie jest udawana.

Paczki `WATAHA_MAP_PACK_V1` mają przyjść jako lokalne pliki (manifest, map.pmtiles, search.db, routing, sha256, signature).
W kryzysie aplikacja nie pobiera nowych paczek.
