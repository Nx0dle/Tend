# Plan prac nad Tend

Repozytorium startuje od jednego bazowego snapshotu istniejącej aplikacji. Kolejne zmiany dzielimy na małe gałęzie i commity według funkcji.

## Stan obecny

- Lokalny budżet miesięczny, kategorie, rezerwa i historia wydatków.
- Start pokazuje bieżący miesiąc i szybkie działanie; Budżet jest osobnym obszarem planowania; Kalendarz jest dziennikiem. Kontrakt produktu opisuje `PROJECT-HANDBOOK.md`.
- Budżet pozwala planować bieżące i przyszłe miesiące, grupuje działania w „Opcjach planu”, kopiuje poprzedni podział limitów (bez puli środków), a przenoszenie salda jest opcjonalne i domyślnie wyłączone; stany planu (historyczny „Tylko podgląd”, przyszły „Z wyprzedzeniem”) komunikowane są zwięzłymi etykietami stanu przy tytule, bez zbędnych tekstów pomocniczych.
- Kalendarz nie wyświetla kwot w komórkach: znaczniki komunikują rozkład wydatków, a szczegóły są w panelu wybranego dnia.
- Spójny język UI jest zdefiniowany tokenami w `TendTheme.kt` (odstępy, promienie, wysokości, ruch, role typograficzne i kolory semantyczne) i udokumentowany w `UI-GUIDELINES.md`. Ekrany Start i Budżet mają własne pliki, a wspólne elementy (hero, pasek alokacji, wiersze) są reużywane.
- Zapisy mają wspólną obsługę błędów z komunikatem w nagłówku zamiast cichej porażki. Schematy Room są eksportowane do `app/schemas` jako punkt odniesienia dla migracji.
- Ciche alerty po wykorzystaniu 80% i 100% limitu kategorii.
- Rachunki cykliczne z przypomnieniem i ręcznym zatwierdzeniem wydatku.
- Aktualizuj `PROJECT-HANDBOOK.md`, `UI-GUIDELINES.md` (gdy dotyczy), README oraz ten plan przy zmianie założeń, funkcji, architektury lub statusu prac.

## Następne segmenty

### 1. Stabilizacja danych lokalnych

Gałąź: `fix/local-data-integrity`

- Przejrzeć migracje Room i zasady usuwania oraz edycji wpisów.
- Ustalić zachowanie przy zmianie miesiąca, spóźnionym przypomnieniu i edycji rachunku po utworzeniu przypomnienia.
- ~~Uporządkować obsługę błędów zapisu tak, by ekran informował o nieudanej operacji.~~ Zrobione: `write` w ViewModelu ustawia komunikat w nagłówku.
- ~~Wyeksportować schematy Room.~~ Zrobione: plugin `androidx.room`, katalog `app/schemas`. Pozostaje dodać testy migracji na bazie tych schematów.

### 2. Projekt synchronizacji szyfrowanej end-to-end

Gałąź: `design/encrypted-sync`

- Ustalić logowanie i identyfikację konta.
- Zaprojektować generowanie, przechowywanie i odzyskiwanie klucza między urządzeniami.
- Określić format zaszyfrowanego snapshotu, wersjonowanie i rozwiązywanie konfliktów.
- Zdefiniować zachowanie przy utracie klucza i wyłączeniu synchronizacji.

### 3. Synchronizacja wielourządzeniowa

Gałąź: `feat/encrypted-sync`

- Dodać konfigurację Firebase bez umieszczania sekretów projektu w repozytorium.
- Wysyłać i pobierać wyłącznie zaszyfrowane dane.
- Dodać opt-in, stan synchronizacji, retry i obsługę pracy offline.
- Zachować lokalne dane i umożliwić wyłączenie synchronizacji.

### 4. Przygotowanie wydania

Gałąź: `chore/release-readiness`

- Uzupełnić instrukcję konfiguracji i budowania.
- Sprawdzić uprawnienia, kopię zapasową danych i zachowanie po aktualizacji.
- Przygotować wersję do dystrybucji.

Każdy segment powinien kończyć się spójnym commitem. Nie łączymy zmian konfiguracji środowiska lokalnego ani artefaktów kompilacji z kodem aplikacji.
