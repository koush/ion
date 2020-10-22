package com.koushikdutta.ion

import android.os.Looper
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.scratch.Promise
import kotlinx.coroutines.CoroutineStart

internal val isMainThread: Boolean
    get() = Looper.getMainLooper().thread == Thread.currentThread()

internal fun requireMainThread() {
    if (!isMainThread)
        throw IllegalStateException("must only call from main thread")
}

internal class BitmapPromise(val lazyPriority: Int? = null, block: suspend() -> BitmapInfo): Promise<BitmapInfo>(start = CoroutineStart.LAZY, block) {
    val isLazyLoad
        get() = lazyPriority != null
    val isLoading
        get() = isStarted

    companion object {
        // atomic safety doesn't matter for access on this, it is just an ordering hint
        var LAZYLOAD_COUNTER = 0

        fun createLazyLoadPriority(): Int {
            requireMainThread()
            val lazyId = LAZYLOAD_COUNTER++;
            return lazyId;
        }

        fun getLazyLoadKey(lazyId: Int, key: String) = "lazyload-${key}-$lazyId"
    }
}
