# Tend

Android personal-expense app, designed around fast entry and monthly category budgets.

## Current milestone

The local budgeting slice is in place: expenses, income, categories, the monthly starting budget, and category limits are stored in Room. Start is for quick expense or income entry and a read-only monthly overview; Budżet is for setting the monthly pool, category limits, and categories. The available pool is the monthly starting budget plus recorded income and carried-over funds. Category limits are assigned from that pool; the reserve is the remainder, so it decreases when limits are assigned and can go below zero if the plan is overallocated. Uncategorised expenses count toward total spending but do not reduce a category limit. A monthly calendar shows daily expense totals and selected-day entries. Month browsing, editing or removing entries and categories, and copying an empty month's plan from the previous month are supported.

Category budget alerts are optional and quiet: the app asks for notification access only after the user turns them on, then sends at most one alert at 80% and one at 100% for each category in the current month. Alert delivery state is stored locally per category and month. Recurring bill reminders can be scheduled by day of month and category; due reminders open an approval list, and only approval adds the expense to the ledger.

Still to implement: optional end-to-end encrypted sync. Android automatic backup is disabled while there is no user-controlled encrypted backup or sync, so reinstalling the app or losing the device can currently lose local data. Add a safe export/recovery path before release.

## Build environment

The project targets JDK 17, Android SDK 36, and Gradle 8.14.4. Gradle is provided by the project wrapper.
