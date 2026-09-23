# Polska Wataha: odbiór i lokalne nagrywanie audio SOS

Model docelowy:
Młoda Wataha -> szyfrowany WebRTC -> Polska Wataha rodzica.

Telefon dziecka wysyła audio dopiero po świadomym użyciu SOS.
Telefon rodzica odbiera prawdziwy zdalny track i dopiero wtedy może rozpocząć lokalne nagrywanie.

Zasady:
- sygnalizacja WebRTC wymaga aktywnego `parent_link`,
- serwer uwierzytelnia obie strony,
- serwer nie archiwizuje audio,
- bez zdalnej ścieżki audio aplikacja pokazuje „Łączenie...” i nie udaje nagrania,
- nagranie zostaje na urządzeniu rodzica,
- mikrofon nie działa jako ukryty podsłuch.

Docelowa nazwa pliku:
`SOS_Imie_RRRR-MM-DD_GG-MM.m4a`

Metadane przy nagraniu:
- sessionId,
- childId,
- startedAt / endedAt,
- ostatnia znana lokalizacja i dokładność,
- sha256 pliku.

UI rodzica:
- alarm SOS,
- imię dziecka,
- ostatnia lokalizacja,
- status łączenia/odbioru/nagrywania,
- licznik czasu,
- „Nawiguj do dziecka”,
- lokalny plik po zakończeniu.

Obecny kod zawiera kontrakty i bezpieczny stan odbiornika. Pełny runtime WebRTC/MediaRecorder nadal musi zostać faktycznie podłączony i przetestowany na urządzeniu.
