package com.tend.wear

import android.content.Context
import android.net.Uri
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

    private val snapshotUri: Uri = Uri.parse("wear://*/${WatchSync.SNAPSHOT_PATH}")

    suspend fun read(context: Context): WatchSnapshot? = withContext(Dispatchers.IO) {
        try {
            val client = Wearable.getDataClient(context.applicationContext)
            val item = Tasks.await(client.getDataItem(snapshotUri))
            val payload = item?.data?.let { DataMap.fromByteArray(it).getString(WatchSync.KEY_PAYLOAD) }
            payload?.let(::decodeWatchSnapshot)
        } catch (_: Exception) {
            // No paired phone yet, or GMS hiccup: show the empty state instead of crashing.
            null
        }
    }
}
