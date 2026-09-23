# Polska Wataha: lokalizacja dziecka

Rodzic/opiekun może widzieć bieżącą pozycję dziecka tylko przez aktywny Family Bridge.

Warunki:
- `parent_link` ma status `active`,
- viewer jest `parent_user_id` tego linku,
- zgoda lokalizacyjna dziecka nie jest `off`,
- backend sprawdza uprawnienia przy każdym odczycie,
- brak publicznego endpointu z dokładną pozycją,
- ADMIN bez relacji rodzic-dziecko nie widzi lokalizacji.

Status pozycji:
- „aktualna”, gdy ostatni fix ma nie więcej niż 3 minuty,
- później „nieaktualna”,
- przy braku internetu można pokazać ostatnią pozycję zapisaną lokalnie, ale aplikacja nie udaje nowej synchronizacji.

UI rodzica:
- karta dziecka,
- „Pokaż na mapie”,
- „Ostatnia aktualizacja: ...”,
- dokładność pozycji,
- status online/offline.

Młoda Wataha:
- widoczny status udostępniania,
- osobna zgoda na pracę w tle,
- możliwość wyłączenia udostępniania,
- SOS może przesłać bieżącą pozycję.

Nie implementować ukrytego trackingu ani publicznego śledzenia.
