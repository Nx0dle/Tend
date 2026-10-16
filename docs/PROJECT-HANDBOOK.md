# Tend — przewodnik po projekcie

Ten dokument jest punktem wejścia dla każdej osoby lub agenta przejmującego pracę nad aplikacją. Zawiera aktualne założenia produktu, działanie funkcji, strukturę kodu i reguły pracy. Gdy zmienia się zachowanie produktu, architektura albo wspólny język UI, zaktualizuj ten dokument, `README.md` oraz właściwy dokument szczegółowy w tym samym zestawie zmian.

## 1. Produkt

Tend to lokalna aplikacja Android do szybkiego zapisywania wydatków i planowania miesięcznego budżetu według kategorii. Główny przepływ: ustawić miesięczną pulę, rozdzielić ją na limity kategorii, dodawać wpisy bez tarcia i sprawdzać faktyczne wydatki względem planu.

To narzędzie do kontroli codziennych finansów, nie bank, portfel inwestycyjny ani aplikacja społecznościowa. Nie dodawaj ekranów, wskaźników czy powiadomień wyłącznie dlatego, że są typowe dla aplikacji finansowych: każda rzecz musi wspierać konkretną decyzję użytkownika.

## 2. Model ekranów

Dolna nawigacja prowadzi do trzech odrębnych zadań. Gest poziomy pozwala przechodzić między nimi.

### Start — bieżący miesiąc i szybki zapis

- Zawsze pokazuje bieżący miesiąc, niezależnie od miesiąca wybranego w Kalendarzu lub Budżecie.
- Służy do dodania wydatku/przychodu oraz szybkiej oceny sytuacji teraz: dzisiejsze wpisy z licznikiem, wykorzystanie limitów kategorii ze zwięzłymi etykietami stanu, ostatnia aktywność i stan bieżącego planu.
- Nie ma nawigacji po miesiącach; jej obecność dublowałaby historię i plan.

### Kalendarz — historia dzienna

- Przegląda wpisy i wydatki według daty. To jedyny ekran dziennika z siatką kalendarza.
- Może przeglądać wybrane miesiące. Nawigacja miesiąca znajduje się we wspólnym nagłówku.
- Zapamiętuje własny oglądany miesiąc niezależnie od okresu w Budżecie, więc przygotowanie przyszłego planu nie przenosi dziennika na pusty przyszły miesiąc.
- Komórki kalendarza pokazują datę oraz dyskretny znacznik wydatku, a nie kwoty transakcji. Dokładne kwoty i lista wpisów są w szczególe wybranego dnia.
- Wpis można otworzyć do edycji; miesiąc z przeszłości nie powinien przez to zmieniać widoku Start.

### Budżet — plan miesięczny

- Przegląda i edytuje pulę oraz limity kategorii dla wybranego miesiąca.
- Miesiąc bieżący i przyszły pozwalają przygotować plan. Przyszły plan pokazuje planowane limity, bez etykiet „wydano/pozostało” ani paska zużycia; przychody, alerty progowe i rachunki cykliczne nie są prognozowane.
- Historyczny miesiąc służy do wglądu; nie edytuj go po cichu.
- Zmiany planu grupuje akcja „Opcje planu”: ustawienie/zmiana/usunięcie puli, opcjonalne przenoszenie salda i kopiowanie limitów kategorii z poprzedniego miesiąca.
- Można skopiować limity kategorii z poprzedniego miesiąca tylko wtedy, gdy docelowy miesiąc nie ma jeszcze limitów. Kopiowanie nie przenosi puli: pula jest nowym zasileniem środków dla konkretnego miesiąca.
- Budżet zapamiętuje swój wybrany miesiąc oddzielnie od Kalendarza. To pozwala wrócić do przyszłego planu bez zmiany okresu dziennika.
- Rezerwa jest resztą po odjęciu sumy limitów kategorii od dostępnej puli; zmniejsza się po przypisaniu limitu i może być ujemna przy nadmiernej alokacji.

### Nawigacja miesiąca

- Jeden wspólny nagłówek dla trzech ekranów. Wysokość nagłówka i miejsce etykiety miesiąca pozostają stałe przy przełączaniu zakładek.
- Strzałki są widoczne tylko w Kalendarzu i Budżecie. Start ma zachować ten sam układ nagłówka bez akcji zmiany miesiąca.
- Nazwa miesiąca po polsku, w jednym wierszu, bez dodatkowej etykiety „bieżący miesiąc”, która przesuwa treść.

## 3. Reguły finansowe i dane

- Kwoty są przechowywane jako całkowite grosze (`Long`), nigdy jako liczby zmiennoprzecinkowe. Formatowanie i parsowanie znajdują się w kodzie UI; waliduj wartości dodatnie tam, gdzie wymagane.
- Wpis ma typ `LedgerEntry.TYPE_EXPENSE`/`TYPE_INCOME` (stałe, nie surowe stringi), kwotę, opcjonalną kategorię, czas zdarzenia i `monthKey` w formacie `YearMonth.toString()` (np. `2026-10`).
- Wydatek bez kategorii wchodzi do sumy wydatków, ale nie zmniejsza limitu żadnej kategorii.
- Dostępna pula miesiąca = zaplanowana pula początkowa + przychody zaksięgowane w tym miesiącu + przeniesione środki, jeśli użytkownik włączył tę opcję.
- Przenoszenie salda jest globalnym ustawieniem (domyślnie wyłączonym). Gdy jest włączone, saldo poprzednich miesięcy liczy się narastająco jako pule tych miesięcy oraz ich przychody minus wydatki. Każdy grosz przechodzi dalej w ramach jednego salda; nie jest ponownie dodawany jako nowa pula.
- Do salda wliczają się tylko miesiące rozpoczęte lub zakończone, do bieżącego miesiąca włącznie. Pula zaplanowana na przyszłość nie zasila wcześniejszego miesiąca ani kolejnych planów, dopóki ten miesiąc się nie rozpocznie.
- Pula miesiąca oznacza nowe środki wpłacone/zaplanowane na ten miesiąc. Nie jest szablonem i nie jest kopiowana wraz z limitami. Rekord puli można usunąć bez usuwania limitów.
- Limity są osobne dla pary kategoria–miesiąc. Suma limitów nie jest automatycznie odejmowana od puli; rezerwa jest wynikiem obliczenia w UI.
- Brak wiersza oznacza „brak limitu” albo „pula nieustawiona”. Zapis limitu lub puli równej zero (lub mniejszej) usuwa wiersz, zamiast tworzyć pusty rekord, dzięki czemu kopiowanie planu i etykiety stanu pozostają poprawne.
- Nieudany zapis (np. błąd dysku) pokazuje użytkownikowi zwięzły komunikat w nagłówku; operacje zapisu nie kończą się cicho.
- Usunięcie kategorii jest transakcyjne: czyści jej referencje w transakcjach i rachunkach/przypomnieniach, limity i stany alertów, a potem kategorię. Wpisy pozostają w księdze jako bez kategorii.
- Usunięcie/zmiana wpisu, budżetu i kategorii musi zachować poprawność historycznych sum i limitów.

## 4. Funkcje istniejące

- Lokalna księga wydatków i przychodów, kategorie wydatków, miesięczna pula i limity kategorii.
- Szybkie dodawanie z ekranu Start; wpisy mają czas bieżący i trafiają do bieżącego miesiąca.
- Kalendarz wydatków z wyborem dnia, listą wpisów i edycją istniejącego wpisu.
- Podsumowanie wydatków po kategoriach z postępem limitów oraz czytelnymi etykietami stanu (`TendBadge`).
- Dodawanie, zmiana nazwy i usuwanie kategorii; edycja/usunięcie wpisu.
- Kopiowanie nieistniejących limitów kategorii z miesiąca poprzedniego, bez puli środków.
- Opcjonalne ciche alerty progów kategorii (80% i limit) dla bieżącego miesiąca. Szczegół techniczny o częstotliwości wysyłania należy do kodu/logiki, a nie do głównej treści UI.
- Rachunki cykliczne z kategorią, kwotą i dniem miesiąca. Gdy termin minie, powstaje propozycja; dopiero ręczne zatwierdzenie zapisuje wydatek. Pominięcie nie dodaje wydatku. Dni przekraczające długość miesiąca są ograniczane do ostatniego dnia.
- Zegarek Wear OS (moduł `wear`): kafelek (Tile) z sumą miesiąca i paskami limitów kategorii oraz przewijalny ekran z pełną listą kategorii — dokładnie jak sekcja kategorii na Start. Widok jest tylko do odczytu i lustrzany wobec bieżącego miesiąca Start; dane trafiają z telefonu przez Wear Data Layer. Telefon pozostaje jedynym miejscem zapisu.

### Zegarek Wear OS (companion)

- Utrzymuj jeden kontrakt danych w module `core` (`WatchSync`): model migawki, kodek JSON, formatowanie kwot i paleta kolorów są wspólne dla telefonu i zegarka. Nie duplikuj ich per moduł.
- Telefon liczy migawkę w `TendViewModel.buildWatchSnapshot` z tych samych strumieni co Start (bieżące wpisy, kategorie, limity, pula, przenoszone saldo) i wypycha ją w `WearSyncManager` ścieżką `WatchSync.SNAPSHOT_PATH`. Baza Room nigdy nie opuszcza telefonu; zegarek dostaje wyłącznie ostatnią migawkę.
- Moduł `wear` używa tego samego `applicationId` (`com.tend.app`) co telefon. Wear Data Layer wymaga zgodnej nazwy pakietu i podpisu po obu stronach; bez tego zegarek nie odczyta danych. Namespace kodu pozostaje `com.tend.wear`.
- Kafelek (`TendTileService`) nie przewija się, więc pokazuje tyle wierszy, ile mieści się na ekranie, plus licznik „+N więcej”, i otwiera aplikację po dotknięciu. Pełna, przewijalna lista kategorii jest w `WearApp`.
- Kafelek musi sam ustawić tło: Tile nie dostaje go od systemu, a paleta Tenda jest jasna (ciemny tusz na porcelanie). Brak `setBackground` = ciemny tekst na czerni i kafelek wygląda na pusty. Tło ustawiamy na głównym `Column` razem z `setWidth/setHeight(expand())`, żeby pokryło cały kafel.
- Dotknięcie kafelka buduj przez `ActionBuilders.LaunchAction` + `ActionBuilders.AndroidActivity`. Przeciążenie `Clickable.Builder.setOnClick(PendingIntent)` rzuca `IllegalStateException` poza `ProtoLayoutScope` — cały `onTileRequest` wtedy pada i kafelek zostaje czarny (był to realny błąd). `LaunchAction` jest `Action`, więc idzie przez `setOnClick(Action)` i nie potrzebuje zasobów z PendingIntentami.
- `onTileRequest` nigdy nie kończy się wyjątkiem: błąd odczytu migawki lub budowy układu spada do stanu „brak danych”, bo wyjątek z kafelka daje pusty, czarny kafel.
- Ekran (`WearApp`) używa `ScalingLazyColumn` z `androidx.wear.compose:compose-foundation`, więc treść skaluje się i zanika przy krawędziach okrągłego ekranu. Przewijanie jest skokowe po obu kanałach — `flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state)` i `rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state)` — a wariant rotary zachowuje haptyczne „tyknięcie” na element. Bez własnego kodu rotacji.
- Układ jest „center‑based”: Wear domyślnie wyśrodkowuje element o indeksie 1, dlatego ekran ustawia `initialCenterItemIndex = 0` i `AutoCenteringParams(itemIndex = 0)`, aby nagłówek trafiał w środek okręgu, a nie pod górny łuk. Nie zmieniaj tych dwóch wartości razem z kolejnością elementów.
- Nagłówek jest wyśrodkowany w poziomie (`horizontalAlignment` + `textAlign`), a teksty trzymaj krótkie, żeby zmieściły się w kole: poziomy `contentPadding` to 16 dp, a poboczne kwoty (pula, podpisy kategorii, kafel) formatuje `formatWatchMoneyCompact` (bez groszy) zamiast pełnego `formatWatchMoney`. Pełna kwota z groszami zostaje w nagłówku jako liczba główna.
- Ikona launchera jest wprost skopiowana z telefonu: te same `drawable-*/ic_launcher_foreground.png` i `ic_launcher_monochrome.png` oraz to samo `mipmap-anydpi-v26/ic_launcher(.round).xml` z `@color/launcher_background`. Nie rysuj własnego znaku „t” — ma być identyczny jak na telefonie.
- Zegarek jest tylko odbiorcą: brak własnej bazy i historii. Gdy proces telefonu jest zabity, migawka nie jest odświeżana; zegarek pokazuje ostatni znany stan.


## 5. Zakres poza obecnym wydaniem

- Szyfrowana synchronizacja end-to-end jest planowana, ale nie jest zaimplementowana.
- Brakuje bezpiecznej, kontrolowanej przez użytkownika ścieżki eksportu/odzyskiwania. Automatyczny backup Androida jest wyłączony, aby nie kopiować nieszyfrowanych danych; reinstalacja lub utrata urządzenia może oznaczać utratę lokalnych danych.
- Nie konfiguruj fikcyjnego backendu, sekretów Firebase ani pozornej synchronizacji. Najpierw zaprojektuj model kluczy, odzyskiwanie, wersjonowanie i konflikty; patrz `docs/ROADMAP.md`.
- Migawka na zegarek jest wypychana tylko, gdy proces aplikacji telefonu działa. Odświeżanie w tle (usługa Data Layer nasłuchująca zmian) oraz odpytywanie na żądanie z zegarka nie są jeszcze zaimplementowane.

## 6. Język wizualny i UX

Szczegółowe, trwałe zasady znajdują się w [`UI-GUIDELINES.md`](UI-GUIDELINES.md). Skrót:

- Kierunek: spokojny, precyzyjny, świeży produkt finansowy; nie domyślny Materialowy szablon i nie dekoracyjny dashboard.
- Paleta: porcelanowe tło, głęboka leśna zieleń dla głównego akcentu, szałwia dla cichych powierzchni, oszczędne mosiężne złoto, atramentowy tekst. Czerwień tylko dla błędów i przekroczeń.
- Znak aplikacji: umiarkowanej wielkości, wyśrodkowane małe „t” w kroju Roboto Bold i kolorze szałwii na leśnozielonym tle, zgodne z narożnym znakiem w UI; launcher ma warstwy adaptive i monochrome w katalogu `app/src/main/res`.
- Wspólne odstępy, promienie, wysokości kontrolek, czasy ruchu i role typograficzne są tokenami `TendSpace`, `TendRadius`, `TendSize`, `TendMotion`, `TendType` z `TendTheme.kt`. Kolory niosące znaczenie (rezerwa, przekroczenie, etykiety na powierzchni hero, podpowiedzi) mają nazwane tokeny w `TendTheme.kt`; nie wpisuj surowych kolorów hex w ekranach.
- Zachowuj czytelność przy dużej liczbie wpisów: nie wciskaj kwot ani pełnych opisów do ciasnych komórek.
- Kontrolki dotykowe mają co najmniej 44 dp pola interakcji. Animacja wyjaśnia zmianę stanu, nie przeszkadza w szybkim zapisie.
- Powiadomienia, etykiety i podpowiedzi mają uzasadniać miejsce realną korzyścią. Nie pokazuj użytkownikowi implementacyjnych limitów czy mechaniki alertów.
- Formularze, selektory i modale mają wyglądać jak Tend. Reużywaj komponentów z `TendDialogs.kt`; unikaj nowych surowych dialogów Material bez powodu.
- Każdy widoczny label ma wnosić nową informację. Nie powtarzaj nazwy miesiąca, sekcji ani wartości w sąsiadujących nagłówkach; stany historyczne („Tylko podgląd”) i planowania w przód („Z wyprzedzeniem”) pokazuj zwięzłym labelem przy tytule zamiast osobnym wierszem czy rozwlekłym tekstem pomocniczym. Unikaj redundantnych dopisków tekstowych pod wskaźnikami (np. kolor paska i kwoty w pełni komunikują zużycie limitu).
- Przed dodaniem wzorca UI sprawdź jego use case, hierarchię informacji, zachowanie przy długiej treści, ekranach o małej wysokości, pustych danych i danych historycznych.

## 7. Architektura i mapa kodu

| Obszar | Plik | Odpowiedzialność |
| --- | --- | --- |
| Android entry point | `app/src/main/java/com/tend/app/MainActivity.kt` | Lifecycle, zbieranie strumieni ViewModelu, uprawnienia i przekazanie danych do UI |
| Shell aplikacji | `.../TendHomeScreen.kt` | Nagłówek, nawigacja miesiąca, host tabów, gesty, dolna nawigacja i wspólna edycja wpisu |
| Ekran Start | `.../TendStartScreen.kt` | Bieżący miesiąc, szybki zapis, kategorie, ostatnie wpisy |
| Ekran Budżet | `.../TendBudgetScreen.kt` | Plan miesiąca, opcje planu, limity kategorii, rachunki cykliczne |
| Współdzielone elementy | `.../TendComponents.kt` | Hero, pasek alokacji, wiersze kategorii i wpisu, kalendarz, formatery |
| Edycja i selektory | `.../TendDialogs.kt` | Dialogi, formularze, wybór kategorii, elementy nawigacji miesiąca |
| UI tokens | `.../TendTheme.kt` | Paleta i kolory semantyczne, Material theme, `TendSpace`, `TendRadius`, `TendSize`, `TendMotion`, `TendType` |
| Stan prezentacji | `.../ui/TendViewModel.kt` | Strumienie obserwacji, akcje aplikacji, wybór miesiąca, powiadomienia budżetowe |
| Encje i Room | `.../data/*.kt` | Tabele, DAO, baza, repozytorium, transakcje i obliczenia domenowe |
| Przypomnienia rachunków | `.../notifications/BillReminder*.kt` | Harmonogram workerów i obsługa powiadomienia/propozycji |
| Alerty budżetowe | `.../notifications/BudgetAlertNotifier.kt` | Kanał i dostarczenie alertu przekroczenia limitu |
| Wspólny kontrakt (telefon↔zegarek) | `core/src/main/java/com/tend/shared/WatchSync.kt` | Model migawki, kodek JSON, formatowanie kwot i paleta wspólne dla obu modułów |
| Synchronizacja na telefonie | `app/src/main/java/com/tend/app/wear/WearSyncManager.kt` | Wypchnięcie migawki bieżącego miesiąca przez Wear Data Layer |
| Zegarek Wear OS | `wear/src/main/java/com/tend/wear/TendTileService.kt`, `.../WearApp.kt`, `.../WatchSnapshotStore.kt` | Kafelek i przewijalny ekran; odczyt migawki z Data Layer |

Stan wpisów/planów jest lokalnie przechowywany w Room. ViewModel udostępnia osobne strumienie bieżącego miesiąca i wybranego miesiąca, dzięki czemu Start nie podąża za historią Budżetu/Kalendarza. Nie przenoś logiki SQL, reguł finansowych ani trwałego zapisu do Composable. Zapis ma przechodzić przez ViewModel → `LedgerRepository` → DAO. Schematy Room są eksportowane do `app/schemas` (plugin `androidx.room`); commituj je razem ze zmianą schematu, bo są punktem odniesienia dla migracji.

## 8. Stack i uruchamianie

- Android, Kotlin, Jetpack Compose + Material 3, Room/KSP, Kotlin Coroutines/Flow i WorkManager.
- Namespace/application id: `com.tend.app`.
- `minSdk 26`, `compileSdk 36`, `targetSdk 36`, Java 17, Gradle wrapper 8.14.4.
- Główny moduł aplikacji: `app`.
- Moduły companion: `core` (biblioteka Androida z kontraktem danych wspólnym dla telefonu i zegarka) oraz `wear` (aplikacja Wear OS: kafelek + ekran). Zegarek używa Compose, `androidx.wear.tiles`, `androidx.wear.compose:compose-foundation` (lista na okrągły ekran + rotacja) i `play-services-wearable` (Wear Data Layer). `wear` ma `minSdk 30`.
- Lokalny build: `./gradlew assembleDebug` (buduje wszystkie moduły).
- Pliki lokalnego środowiska (`local.properties`, SDK paths, sekrety) nie powinny wchodzić do commitów.

## 9. Workflow dla kolejnych zmian

1. Zacznij od tego przewodnika, `README.md`, `docs/UI-GUIDELINES.md`, `docs/ROADMAP.md` oraz aktualnego kodu; pamięć rozmowy nie jest specyfikacją.
2. Zapisz, który problem użytkownika zmieniasz i do którego ekranu/warstwy należy.
3. Zanim dodasz funkcję lub element UI, oceń czy wspiera główny przepływ Tend, czy dubluje istniejącą funkcję, jak działa z pustymi i historycznymi danymi oraz czy jest wart złożoności.
4. Trzymaj odpowiedzialności w odpowiednich plikach/warstwach. Jeśli kod nowej funkcji rośnie, wyodrębnij komponent lub plik — nie wracaj do monolitu.
5. Reużywaj tokenów i komponentów. Jeśli świadomie wprowadzisz nową zasadę, zaktualizuj `TendTheme.kt`/`UI-GUIDELINES.md`.
6. Aktualizuj ten handbook, README i roadmapę w tej samej zmianie, jeśli zmieniły się założenia, zachowanie, architektura, obsługa danych, zależności albo status funkcji.
7. Zmiany danych sprawdź pod kątem migracji Room, danych starszych wersji, transakcyjności, duplikatów i referencji. Nie zmieniaj schematu bez sprawdzenia `TendDatabase.kt`.
8. Po implementacji uruchom adekwatną kompilację lub weryfikację, jeśli środowisko pozwala; raportuj uczciwie ograniczenia.
9. Przed końcem sprawdź `git diff`, usuń przypadkowe pliki/sekrety/artefakty i podsumuj zmianę, weryfikację oraz ryzyka.

## 10. Decyzje i historia produktu

Najważniejsze decyzje wynikające z wymagań właściciela:

- Start, Kalendarz i Budżet nie są trzema widokami tego samego okresu: Start służy działaniu w bieżącym miesiącu; Kalendarz ma niezależny miesiąc dziennika; Budżet ma niezależny miesiąc planu.
- Nawigacja Budżetu w przyszłość ma konkretny cel: przygotowanie puli i limitów przed rozpoczęciem miesiąca, z opcją skopiowania poprzedniego podziału limitów. Pusty przyszły miesiąc pokazuje akcje ustawienia puli i skopiowania limitów; kopiowanie nie może dublować środków przenoszonych z poprzedniego miesiąca.
- Nawigacja miesiąca ma sens w Budżecie i Kalendarzu, nie na Start. Wspólny nagłówek nie zmienia jednak położenia przy przełączaniu kart.
- Kalendarz ma rozwijać obecny kierunek całej aplikacji; stary, generyczny grid z kwotami w każdej komórce jest odrzucony ze względu na skalowanie i czytelność.
- Spójność tokenów, odstępów, promieni i ruchu jest zasadą na poziomie projektu, nie jednorazowym retuszem.
- Nowe funkcje oceniamy przed implementacją pod kątem realnego use case i kosztu poznawczego. Nie dodajemy „typowych” elementów bez uzasadnienia.
