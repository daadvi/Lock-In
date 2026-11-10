package com.example.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** One running update and one pending update, even when many timers emit together. */
internal class WidgetUpdateQueue(scope: CoroutineScope, update: suspend (Context) -> Unit) {
    private val requests = Channel<Context>(Channel.CONFLATED)
    init {
        scope.launch {
            for (context in requests) update(context)
        }.invokeOnCompletion { requests.cancel() }
    }
    fun request(context: Context) {
        requests.trySend(context)
    }
}
