# WILK: aktualizacje wiedzy online, odporność offline

WILK w Polskiej Watasze pozostaje local-first.

W trybie normalnym, przy dostępie do internetu, może sprawdzać podpisane paczki wiedzy dotyczące:
- prawa,
- pierwszej pomocy,
- procedur kryzysowych,
- praw obywatelskich,
- bezpieczeństwa.

WILK nie ma samodzielnie uczyć się z przypadkowych stron.

Każdy pakiet wiedzy musi mieć:
- wersję,
- datę,
- listę źródeł,
- SHA-256,
- podpis,
- oznaczenie domeny i krytyczności.

W Trybie Kryzysowym sieć nie jest używana do aktualizacji. WILK korzysta z ostatniej zweryfikowanej lokalnej wersji.

Aktualizacja musi być atomowa: nowa paczka staje się aktywna dopiero po pełnej walidacji. Poprzednia działająca wersja zostaje jako rollback.
