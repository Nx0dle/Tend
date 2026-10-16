package com.tend.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.tend.shared.WatchSync
import com.tend.shared.WatchSnapshot
import com.tend.shared.decodeWatchSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the last snapshot the phone pushed over the Wear Data Layer. */
object WatchSnapshotStore {

    private const val TAG = "TendWear"

    suspend fun read(context: Context): WatchSnapshot? = withContext(Dispatchers.IO) {
        try {
            val client = Wearable.getDataClient(context.applicationContext)
            // getDataItems() is the most reliable read: it lists every item this app can see,
            // independent of the "wear://*/path" host-wildcard resolution.
            val items = Tasks.await(client.getDataItems())
            val uris = items.map { it.uri.toString() }
            Log.i(TAG, "read: ${items.count()} data item(s) $uris")
            val item = items.firstOrNull { it.uri.path == WatchSync.SNAPSHOT_PATH }
            val payload = item?.data?.let { DataMap.fromByteArray(it).getString(WatchSync.KEY_PAYLOAD) }
            Log.i(TAG, "read: payload=${payload?.length ?: -1} chars")
            payload?.let(::decodeWatchSnapshot)
        } catch (throwable: Throwable) {
            // No paired phone yet, or GMS hiccup: show the empty state instead of crashing.
            Log.w(TAG, "read failed", throwable)
            null
        }
    }
}
