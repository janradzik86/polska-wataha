# Polska Wataha: odbiór i lokalne nagrywanie audio SOS

Model:
Młoda Wataha -> szyfrowany WebRTC -> Polska Wataha rodzica.

Telefon dziecka wysyła audio na żywo po użyciu SOS.
Telefon rodzica odbiera strumień i zapisuje nagranie lokalnie.

Serwer:
- uwierzytelnia obie strony,
- sprawdza aktywny parent_link,
- obsługuje sygnalizację WebRTC i push,
- NIE przechowuje audio.

UI rodzica:
- alarm SOS,
- imię dziecka,
- ostatnia lokalizacja,
- „Łączenie z mikrofonem dziecka…”,
- „Odbieram dźwięk”,
- „Nagrywanie na tym urządzeniu”,
- licznik czasu,
- przycisk „Nawiguj do dziecka”,
- po zakończeniu lokalny plik z datą i identyfikatorem sesji.

Nagrywanie nie jest ukryte po stronie dziecka. Android powinien pokazywać aktywne użycie mikrofonu zgodnie z mechanizmami systemowymi.
