package com.koushikdutta.ion.builder

import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.PromiseHelper
import java.util.concurrent.TimeUnit

class IonPromise<T>(wrapped: Promise<T>) : Promise<T>(wrapped.deferred) {
    @Deprecated("This method blocks the thread and is not recommended. Use carefully.")
    fun get(): T {
        return PromiseHelper.get(this)
    }

    @Deprecated("This method blocks the thread and is not recommended. Use carefully.")
    fun get(time: Long, timeUnit: TimeUnit): T {
        return PromiseHelper.get(this, time, timeUnit)
    }
}
