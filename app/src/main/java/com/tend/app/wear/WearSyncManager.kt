package com.tend.app.wear

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.tend.shared.WatchSync
import com.tend.shared.WatchSnapshot
import com.tend.shared.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pushes the current-month [WatchSnapshot] to the paired Wear OS device over the Wear Data Layer.
 * The Room database stays phone-only; the watch only ever sees the last payload we write here.
 */
class WearSyncManager(private val context: Context) {

    private val dataClient by lazy { Wearable.getDataClient(context.applicationContext) }

    /** Best-effort push. Failures (no paired watch, GMS unavailable) are ignored on purpose. */
    suspend fun push(snapshot: WatchSnapshot) {
        withContext(Dispatchers.IO) {
            try {
                val request = PutDataRequest.create(WatchSync.SNAPSHOT_PATH).apply {
                    setData(
                        DataMap().apply {
                            putString(WatchSync.KEY_PAYLOAD, snapshot.toJson())
                            putLong(WatchSync.KEY_GENERATED_AT, snapshot.generatedAtEpochMillis)
                        }.toByteArray(),
                    )
                }
                Tasks.await(dataClient.putDataItem(request))
            } catch (_: Exception) {
                // The watch is a mirror; a failed sync must never disturb phone-side edits.
            }
        }
    }
}
