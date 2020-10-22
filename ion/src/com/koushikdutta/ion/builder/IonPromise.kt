package com.koushikdutta.ion.builder

import com.koushikdutta.scratch.AsyncAffinity
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.PromiseHelper
import java.util.concurrent.TimeUnit

class IonPromise<T>(val affinity: AsyncAffinity?, wrapped: Promise<T>) : Promise<T>(wrapped.deferred) {
    @Deprecated("This method blocks the thread and is not recommended. Use carefully.")
    fun get(): T {
        return PromiseHelper.get(this)
    }

    @Deprecated("This method blocks the thread and is not recommended. Use carefully.")
    fun get(time: Long, timeUnit: TimeUnit): T {
        return PromiseHelper.get(this, time, timeUnit)
    }

    override suspend fun await(): T {
        try {
            return super.await()
        }
        finally {
            affinity?.await()
        }
    }
}
