# Tend

Android personal-expense app, designed around fast entry and monthly category budgets.

## Current milestone

The local budgeting slice is in place: expenses, income, categories, the monthly starting budget, and category limits are stored in Room. The home screen supports quick expense or income entry, optional category assignment, category creation, and monthly limit editing. The reserve includes prior unspent balances and current income, less the category allocations. A monthly calendar shows daily expense totals and selected-day entries. Month browsing, editing or removing entries and categories, and copying an empty month's plan from the previous month are supported.

Category budget alerts are optional and quiet: the app asks for notification access only after the user turns them on, then sends at most one alert at 80% and one at 100% for each category in the current month. Alert delivery state is stored locally per category and month. Recurring bill reminders can be scheduled by day of month and category; due reminders open an approval list, and only approval adds the expense to the ledger.

Still to implement: optional end-to-end encrypted sync.

## Build environment

The project targets JDK 17, Android SDK 36, and Gradle 8.14.4. Gradle is provided by the project wrapper.
