# AGENTS.md — Zasady pracy nad projektem

Standard dla agentów (opencode i inności) oraz ludzi pracujących nad Tend. Szczegóły projektowe są w `docs/PROJECT-HANDBOOK.md`; tu są wyłącznie zasady nakładu pracy: branch, commit, PR.

## Workflow: branch-first

- **Praca na apce (kod) zaczyna się od nowego brancha.** Żadnych commitów bezpośrednio na `main`. `main` jest stabilne: kompiluje się, dane użytkownika (Room) się nie psują.
- Wyjątek: zmiany czysto dokumentacyjne (`docs/`, `md` w root) mogą iść wprost na `main` jako pojedynczy commit `docs: …`.
- Jeden branch = jeden temat z listy TODO. Nie mieszaj tematów; kolejne tematy → kolejne branche z bieżącego `main`.
- Branch stworz odświeżony: `git checkout main && git pull` przed startem.

## Naming brancha

Format: `<typ>/<scope>-<slug>` — spójny z conventional commits używanymi w repo.

- `typ` = `feat` | `fix` | `docs` | `refactor` | `test` | `chore`
- `scope` = moduł/obszar: `app`, `wear`, `core`, `calendar`, `budget`, `nav`, `sync`, `docs` …
- `slug` = krótki, angielski, kebab-case, bez numerów biletów (brak trackera); max ~5 słów.

Przykłady:

```
feat/budget-live-reserve
fix/calendar-day-income
feat/wear-tile-list
docs/pr-template
```

## Commity

- Conventional commits, małe, jeden temat na commit: `fix(budget): …`, `feat(nav): …`, `docs: …`.
- Przy zmianach w wielu obszarach jedną robotą — segmentuj: selektywne staging (`git add -p` lub patche), tak by każdy commit kompilował się samodzielnie.
- Przed commitem: `git status`, `git diff` — tylko zamierzone pliki, bez sekretów i artefaktów (`build/`, `local.properties`).

## Weryfikacja przed PR

- `./gradlew :app:compileDebugKotlin` (i `:wear:compileDebugKotlin` przy dotyku wear/core).
- Testy, jeśli dotyczyły zmiany; przy zmianach UI — instalacja na urządzeniu: `./gradlew :app:installDebug` i ręczny podgląd.
- Instalacja przez update nigdy nie może wymusić reinstalacji z utratą danych (uwaga na zmianę podpisu/keystore).

## Urządzenia użytkownika

- Na podłączonym urządzeniu (telefon, zegarek) agent **ma prawo wykonywać wyłącznie instalację apk z tego projektu** (`./gradlew :app:installDebug` / `:wear:installDebug`).
- Wszystko inne przez adb — uruchamianie/stopping aplikacji, `pull/push`, backup, odinstalowanie, logcat, ustawienia, testy na urządzeniu — **tylko za jawną zgodą użytkownika**, zanim komenda pójdzie.
- Zabronione bez pytania: cokolwiek naruszające dane użytkownika (uninstall, clear, wipe) — nawet gdy wydaje się niezbędne (np. konflikt podpisów): najpierw opisz sytuację i zaproponuj plan, czekaj na akceptację.

## Template opisu PR

Jeden spójny szablon dla każdego PR (kolejność sekcji stała, tytuł PR = conventional commit):

```markdown
## Cel
<Jedno zdanie: co rozwiązuje i dlaczego. Odniesienie do pozycji z TODO, jeśli jest.>

## Zmiany
- <komponent/plik: co się zmieniło i dlaczego — punkt po punkcie>

## Weryfikacja
- [ ] `./gradlew :app:compileDebugKotlin` + [testy/dotknięte moduły]
- [ ] Instalacja na urządzeniu / emulatorze — co sprawdzono ręką
- [ ] Dane użytkownika (Room) nietknięte — instalacja przez update

## Ryzyka i decyzje
- <co mogło zostać naruszone, co świadomie zostawione — w tym alternatywy z odrzucone>

## Typ brancha
<typ>/<scope>-<slug> — zgodność z konwencją namingu.
```

- PR krótko i konkretnie; sekcje „Ryzyka i decyzje" nie może być „brak" bez jednego sensownego zdania uzasadnienia (co sprawdzono).
- Merge: squash na `main` (domyślnie), język opisu — polski, tytuł — konwencja commita.
