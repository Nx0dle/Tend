package com.tend.app.data

import androidx.room.Entity

@Entity(tableName = "category_budget_alerts", primaryKeys = ["categoryId", "monthKey"])
data class CategoryBudgetAlertState(
    val categoryId: String,
    val monthKey: String,
    val warningSent: Boolean = false,
    val limitSent: Boolean = false,
)

data class BudgetAlertEvent(
    val categoryId: String,
    val monthKey: String,
    val categoryName: String,
    val warningReached: Boolean,
    val limitReached: Boolean,
)
