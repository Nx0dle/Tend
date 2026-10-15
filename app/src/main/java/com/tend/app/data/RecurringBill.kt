package com.tend.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_bills")
data class RecurringBill(
    @PrimaryKey val id: String,
    val name: String,
    val amountCents: Long,
    val categoryId: String?,
    val dayOfMonth: Int,
    val enabled: Boolean = true,
)
