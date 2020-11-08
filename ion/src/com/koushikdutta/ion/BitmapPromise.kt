package com.koushikdutta.ion

import android.os.Looper
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.ion.builder.IonPromise
import com.koushikdutta.scratch.AsyncAffinity
import com.koushikdutta.scratch.Promise
import kotlinx.coroutines.CoroutineStart

internal val isMainThread: Boolean
    get() = Looper.getMainLooper().thread == Thread.currentThread()

internal fun requireMainThread() {
    if (!isMainThread)
        throw IllegalStateException("must only call from main thread")
}

internal class BitmapPromise(contextReference: IonContext? = null, val lazyPriority: Int? = null, affinity: AsyncAffinity?, block: suspend() -> BitmapInfo): IonPromise<BitmapInfo>(contextReference, affinity, CoroutineStart.LAZY, block) {
    val isLazyLoad
        get() = lazyPriority != null
    val isLoading
        get() = isStarted

    init {
        cancelSilently = false
    }

    companion object {
        var LAZYLOAD_COUNTER = 0

        fun createLazyLoadPriority(): Int {
            requireMainThread()
            val lazyId = LAZYLOAD_COUNTER++;
            return lazyId;
        }

        fun getLazyLoadKey(lazyId: Int, key: String) = "lazyload-${key}-$lazyId"
    }
}
