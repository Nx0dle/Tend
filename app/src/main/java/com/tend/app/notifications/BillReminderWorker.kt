package com.tend.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tend.app.data.LedgerRepository
import com.tend.app.data.TendDatabase
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

class BillReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val repository = LedgerRepository(TendDatabase.get(applicationContext))
        val dueBills = repository.refreshBillOccurrences(LocalDate.now())
        val notifier = BillReminderNotifier(applicationContext)
        dueBills.forEach { notifier.notify(it.bill, it.monthKey) }
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }
}
