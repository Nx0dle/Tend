package com.tend.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LedgerEntry::class, ExpenseCategory::class, CategoryBudget::class, MonthlyBudget::class, CategoryBudgetAlertState::class, RecurringBill::class, BillReminderOccurrence::class], version = 6, exportSchema = true)
abstract class TendDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var instance: TendDatabase? = null

        fun get(context: Context): TendDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TendDatabase::class.java,
                "tend.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS expense_categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS category_budgets (categoryId TEXT NOT NULL, monthKey TEXT NOT NULL, amountCents INTEGER NOT NULL, PRIMARY KEY(categoryId, monthKey))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_category_budgets_monthKey ON category_budgets(monthKey)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS monthly_budgets (monthKey TEXT NOT NULL PRIMARY KEY, plannedCents INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS category_budget_alerts (categoryId TEXT NOT NULL, monthKey TEXT NOT NULL, warningSent INTEGER NOT NULL, limitSent INTEGER NOT NULL, PRIMARY KEY(categoryId, monthKey))")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS recurring_bills (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, amountCents INTEGER NOT NULL, categoryId TEXT, dayOfMonth INTEGER NOT NULL, enabled INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS bill_reminders (billId TEXT NOT NULL, monthKey TEXT NOT NULL, name TEXT NOT NULL, amountCents INTEGER NOT NULL, categoryId TEXT, dueAtEpochMillis INTEGER NOT NULL, status TEXT NOT NULL, PRIMARY KEY(billId, monthKey))")
            }
        }

        /** Proposals become pure links to the recurring bill; statuses migrate over, snapshots drop. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS bill_occurrences_v6 (billId TEXT NOT NULL, monthKey TEXT NOT NULL, status TEXT NOT NULL, notifiedAtEpochMillis INTEGER, PRIMARY KEY(billId, monthKey))",
                )
                db.execSQL(
                    "INSERT INTO bill_occurrences_v6 (billId, monthKey, status, notifiedAtEpochMillis) SELECT billId, monthKey, status, NULL FROM bill_reminders",
                )
                db.execSQL("DROP TABLE bill_reminders")
                db.execSQL("ALTER TABLE bill_occurrences_v6 RENAME TO bill_reminders")
            }
        }
    }
}
