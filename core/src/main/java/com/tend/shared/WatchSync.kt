package com.tend.shared

import java.text.NumberFormat
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contract shared by the phone pusher and the watch reader. The phone owns the Room database and
 * computes the snapshot; the watch only renders the last payload it received over the Wear Data Layer.
 */
object WatchSync {
    /** Data Layer path the phone writes and the watch app/Tile read. */
    const val SNAPSHOT_PATH = "/tend/current-month"

    /** DataMap key holding the JSON-encoded [WatchSnapshot]. */
    const val KEY_PAYLOAD = "payload"

    /** DataMap key holding the snapshot timestamp, for "last synced" copy. */
    const val KEY_GENERATED_AT = "generatedAt"
}

/** One category row: spent so far, its monthly limit (0 means no limit), and the phone accent color. */
data class WatchCategory(
    val id: String,
    val name: String,
    val spentCents: Long,
    val limitCents: Long,
    val colorArgb: Long,
)

/**
 * Current-month snapshot rendered on the watch. Field names mirror the phone Start tab so the watch
 * shows the same values: total spent, the available pool split, and per-category progress.
 */
data class WatchSnapshot(
    val monthKey: String,
    val totalSpentCents: Long,
    val availableCents: Long,
    val allocatedCents: Long,
    val reserveCents: Long,
    val hasPool: Boolean,
    val todaySpentCents: Long,
    val generatedAtEpochMillis: Long,
    val categories: List<WatchCategory>,
)

private val PolishLocale = Locale.forLanguageTag("pl-PL")

/** Same Polish currency formatting as the phone (`TendComponents.formatMoney`). */
fun formatWatchMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance(PolishLocale).format(cents / 100.0)

/**
 * Whole-złoty variant (`7 700 zł`) for layouts with very little room, such as the round watch
 * screen and the Tile. The hero total keeps the full precision of [formatWatchMoney].
 */
fun formatWatchMoneyCompact(cents: Long): String = CompactPolishCurrency.format(cents / 100.0)

private val CompactPolishCurrency: NumberFormat =
    NumberFormat.getCurrencyInstance(PolishLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

/**
 * Single source for category accents, mirroring the phone palette so a category keeps its color on
 * the watch. Values are ARGB longs.
 */
object TendPalette {
    const val PORCELAIN = 0xFFF5F3EC
    const val INK = 0xFF18332D
    const val FOREST = 0xFF176451
    const val DEEP_FOREST = 0xFF103E35
    const val SAGE = 0xFFDCEBE2
    const val GOLD = 0xFFC69A52
    const val MUTED = 0xFF64766F
    const val LINE = 0xFFDCE4DE
    const val DANGER = 0xFFB6534A
    const val NOTICE = 0xFF8B6420

    private val CategoryColors = longArrayOf(
        0xFF176451, 0xFF397561, 0xFF73A58A, 0xFFC69A52,
        0xFFD6B36B, 0xFFC87963, 0xFF69818A, 0xFF98A284,
    )

    fun colorFor(categoryId: String?): Long =
        if (categoryId == null) MUTED else CategoryColors[(categoryId.hashCode() and Int.MAX_VALUE) % CategoryColors.size]
}

/** Deadline limit as a fraction of the limit, clamped to [0, 1]. 0 when there is no limit. */
fun WatchCategory.limitRatio(): Float =
    if (limitCents > 0L) (spentCents.toFloat() / limitCents).coerceIn(0f, 1f) else 0f

fun WatchSnapshot.toJson(): String {
    val root = JSONObject()
    root.put("monthKey", monthKey)
    root.put("totalSpent", totalSpentCents)
    root.put("available", availableCents)
    root.put("allocated", allocatedCents)
    root.put("reserve", reserveCents)
    root.put("hasPool", hasPool)
    root.put("todaySpent", todaySpentCents)
    root.put("generatedAt", generatedAtEpochMillis)
    val array = JSONArray()
    for (category in categories) {
        array.put(
            JSONObject().apply {
                put("id", category.id)
                put("name", category.name)
                put("spent", category.spentCents)
                put("limit", category.limitCents)
                put("color", category.colorArgb)
            },
        )
    }
    root.put("categories", array)
    return root.toString()
}

/** Returns null when the payload is missing or malformed, so callers can show an empty state. */
fun decodeWatchSnapshot(json: String): WatchSnapshot? = try {
    val root = JSONObject(json)
    val array = root.optJSONArray("categories") ?: JSONArray()
    val categories = ArrayList<WatchCategory>(array.length())
    for (index in 0 until array.length()) {
        val item = array.getJSONObject(index)
        categories += WatchCategory(
            id = item.getString("id"),
            name = item.getString("name"),
            spentCents = item.getLong("spent"),
            limitCents = item.getLong("limit"),
            colorArgb = item.getLong("color"),
        )
    }
    WatchSnapshot(
        monthKey = root.getString("monthKey"),
        totalSpentCents = root.getLong("totalSpent"),
        availableCents = root.getLong("available"),
        allocatedCents = root.getLong("allocated"),
        reserveCents = root.getLong("reserve"),
        hasPool = root.getBoolean("hasPool"),
        todaySpentCents = root.getLong("todaySpent"),
        generatedAtEpochMillis = root.getLong("generatedAt"),
        categories = categories,
    )
} catch (_: Exception) {
    null
}
