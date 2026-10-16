package com.tend.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tend.app.notifications.BillReminderWorker
import com.tend.app.ui.TendViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(
            BILL_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BillReminderWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                .setInitialDelay(15, java.util.concurrent.TimeUnit.MINUTES)
                .build(),
        )
        workManager.enqueueUniqueWork(
            BILL_REMINDER_STARTUP_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BillReminderWorker>().build(),
        )
        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            var notificationsEnabled by remember { mutableStateOf(hasNotificationAccess(context)) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                notificationsEnabled = granted && hasNotificationAccess(context)
                if (notificationsEnabled) viewModel.refreshBudgetAlerts()
            }
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        notificationsEnabled = hasNotificationAccess(context)
                        viewModel.refreshCurrentMonth()
                        if (notificationsEnabled) viewModel.refreshBudgetAlerts()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            val entries by viewModel.entries.collectAsStateWithLifecycle()
            val currentEntries by viewModel.currentEntries.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val budgets by viewModel.budgets.collectAsStateWithLifecycle()
            val currentBudgets by viewModel.currentBudgets.collectAsStateWithLifecycle()
            val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
            val currentMonthlyBudget by viewModel.currentMonthlyBudget.collectAsStateWithLifecycle()
            val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
            val copyPlanMessage by viewModel.copyPlanMessage.collectAsStateWithLifecycle()
            val carryover by viewModel.carryover.collectAsStateWithLifecycle()
            val currentCarryover by viewModel.currentCarryover.collectAsStateWithLifecycle()
            val carryoverEnabled by viewModel.carryoverEnabled.collectAsStateWithLifecycle()
            val recurringBills by viewModel.recurringBills.collectAsStateWithLifecycle()
            val pendingBillReminders by viewModel.pendingBillReminders.collectAsStateWithLifecycle()
            TendTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Porcelain) {
                    TendHomeScreen(
                        entries = entries,
                        currentEntries = currentEntries,
                        categories = categories,
                        budgets = budgets,
                        currentBudgets = currentBudgets,
                        monthlyBudget = monthlyBudget,
                        currentMonthlyBudget = currentMonthlyBudget,
                        selectedMonth = selectedMonth,
                        onMonthChange = viewModel::showMonth,
                        onAddEntry = viewModel::addEntry,
                        onAddCategory = viewModel::addCategory,
                        onSetCategoryBudget = viewModel::setCategoryBudget,
                        onSetMonthlyBudget = viewModel::setMonthlyBudget,
                        onClearMonthlyBudget = viewModel::clearMonthlyBudget,
                        onCopyPreviousPlan = viewModel::copyPreviousPlan,
                        copyPlanMessage = copyPlanMessage,
                        carryoverCents = carryover,
                        currentCarryoverCents = currentCarryover,
                        carryoverEnabled = carryoverEnabled,
                        onSetCarryoverEnabled = viewModel::setCarryoverEnabled,
                        onRenameCategory = viewModel::renameCategory,
                        onDeleteCategory = viewModel::deleteCategory,
                        onUpdateEntry = viewModel::updateEntry,
                        onDeleteEntry = viewModel::deleteEntry,
                        notificationsEnabled = notificationsEnabled,
                        onEnableNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                )
                            }
                        },
                        openBillsTab = intent.getBooleanExtra(EXTRA_OPEN_BILLS, false),
                        recurringBills = recurringBills,
                        pendingBillReminders = pendingBillReminders,
                        onSaveRecurringBill = viewModel::saveRecurringBill,
                        onDeleteRecurringBill = viewModel::deleteRecurringBill,
                        onApproveBillReminder = viewModel::approveBillReminder,
                        onSkipBillReminder = viewModel::skipBillReminder,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_BILLS = "com.tend.app.OPEN_BILLS"
        private const val BILL_REMINDER_WORK = "tend_bill_reminders_daily"
        private const val BILL_REMINDER_STARTUP_WORK = "tend_bill_reminders_startup"
    }
}


internal fun hasNotificationAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false
    return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).areNotificationsEnabled()
}
