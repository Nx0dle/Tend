# Tend

Android personal-expense app, designed around fast entry and monthly category budgets.

Before making product or UI changes, read [`docs/PROJECT-HANDBOOK.md`](docs/PROJECT-HANDBOOK.md). It is the maintained handoff document for product assumptions, behavior, architecture, and contributor workflow. Shared visual rules are in [`docs/UI-GUIDELINES.md`](docs/UI-GUIDELINES.md).

## Current milestone

The local budgeting slice is in place: expenses, income, categories, the monthly starting budget, and category limits are stored in Room. Start is a current-month dashboard for quick entry, today's activity, category limit progress with status badges, and recent entries; it never switches to a historical month. Calendar is the dated ledger and browses months. Budget is the monthly planning workspace: it browses a plan month and owns changes to the pool, category limits, and categories. The available pool is the monthly starting budget plus recorded income and, if enabled in Plan Options, carried-over balance. Carryover is off by default; future planned pools are excluded until those months begin. Plan Options groups pool editing/removal, category-limit copying, and this carryover setting. The UI avoids captions that merely repeat the visible month, section, or value; plan status badges (such as past read-only or advance planning) sit beside the plan heading. Category limits are assigned from the available pool; the reserve is the remainder, so it decreases when limits are assigned and can go below zero if the plan is overallocated. Uncategorised expenses count toward total spending but do not reduce a category limit. Month browsing, editing or removing entries and categories, and copying an empty month's category limits from the previous month are supported. Copying never duplicates a pool.

Category budget alerts are optional and quiet: the app asks for notification access only after the user turns them on, then sends at most one alert at 80% and one at 100% for each category in the current month. Alert delivery state is stored locally per category and month. Recurring bill reminders can be scheduled by day of month and category; due reminders open an approval list, and only approval adds the expense to the ledger.

Still to implement: optional end-to-end encrypted sync. Android automatic backup is disabled while there is no user-controlled encrypted backup or sync, so reinstalling the app or losing the device can currently lose local data. Add a safe export/recovery path before release.

## Build environment

The project targets JDK 17, Android SDK 36, and Gradle 8.14.4. Gradle is provided by the project wrapper.

## UI code map

- `MainActivity.kt` wires Android lifecycle, permissions, ViewModel streams, and the root theme.
- `TendHomeScreen.kt` owns tab navigation and the three distinct screen flows.
- `TendComponents.kt` contains calendar, category summaries, charts, and shared display rows.
- `TendDialogs.kt` contains editing flows and the custom Tend dialog/input components.
- `TendTheme.kt` defines the app palette, spacing, shape, motion tokens, and Material theme.

The persistent visual and interaction rules live in [`docs/UI-GUIDELINES.md`](docs/UI-GUIDELINES.md). Treat it as the source of truth when adding or changing UI.

## Keeping project knowledge current

When a change introduces or alters product behavior, data rules, architecture, dependencies, or UI conventions, update `docs/PROJECT-HANDBOOK.md`, `docs/UI-GUIDELINES.md` when relevant, this README, and `docs/ROADMAP.md` when milestone status changes in the same change set.
