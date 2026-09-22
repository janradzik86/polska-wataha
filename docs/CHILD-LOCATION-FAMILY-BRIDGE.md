# Lokalizacja dziecka

Widok rodzica dostaje pozycję tylko gdy:

- parent_link ma status active
- viewer jest parent_user_id tego linku
- child_location_consent nie jest off

ADMIN nie widzi cudzych dzieci.

Status pozycji: aktualna, gdy ostatni fix ma mniej niż 3 minuty. W przeciwnym razie nieaktualna.

Brak internetu: ostatni znany fix może zostać na telefonie rodzica. Nowy sync nie jest udawany.
