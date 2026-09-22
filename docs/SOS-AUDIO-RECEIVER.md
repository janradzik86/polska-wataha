# Odbiornik SOS audio

Dźwięk ma iść na żywo (WebRTC) i być nagrany na telefonie rodzica.

`SosAudioReceiver` nie uruchamia MediaRecorder, dopóki nie ma zdalnej ścieżki audio.
Serwer nie archiwizuje dźwięku. Sygnalizacja (offer, answer, ICE) należy do Family Bridge i wymaga aktywnego parent_link.

Nazwa pliku: SOS_Imie_RRRR-MM-DD_GG-MM.m4a plus metadane sessionId, childId, startedAt, endedAt, lastKnownLat, lastKnownLon, accuracy, sha256 — zapisywane obok pliku na urządzeniu rodzica, gdy nagranie naprawdę powstanie.
