package com.tend.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "category_budgets",
    primaryKeys = ["categoryId", "monthKey"],
    indices = [Index("monthKey")],
)
data class CategoryBudget(
    val categoryId: String,
    val monthKey: String,
    val amountCents: Long,
)
