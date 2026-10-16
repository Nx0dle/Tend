package com.tend.app.wear

import android.content.Context
import android.util.Log
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
    private val nodeClient by lazy { Wearable.getNodeClient(context.applicationContext) }

    /** Best-effort push. Failures (no paired watch, GMS unavailable) are ignored on purpose. */
    suspend fun push(snapshot: WatchSnapshot) {
        withContext(Dispatchers.IO) {
            try {
                val nodeCount = runCatching { Tasks.await(nodeClient.connectedNodes).size }.getOrDefault(-1)
                val request = PutDataRequest.create(WatchSync.SNAPSHOT_PATH).apply {
                    setData(
                        DataMap().apply {
                            putString(WatchSync.KEY_PAYLOAD, snapshot.toJson())
                            putLong(WatchSync.KEY_GENERATED_AT, snapshot.generatedAtEpochMillis)
                        }.toByteArray(),
                    )
                }
                val item = Tasks.await(dataClient.putDataItem(request))
                Log.i(
                    TAG,
                    "push ok uri=${item.uri} bytes=${item.data?.size} " +
                        "categories=${snapshot.categories.size} nodes=$nodeCount",
                )
            } catch (throwable: Throwable) {
                // The watch is a mirror; a failed sync must never disturb phone-side edits.
                Log.w(TAG, "push failed nodes/putDataItem threw", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "TendWear"
    }
}
