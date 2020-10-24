package com.koushikdutta.ion.builder

import com.koushikdutta.scratch.AsyncAffinity
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.PromiseHelper
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import java.util.concurrent.TimeUnit

const val THREAD_BLOCK_WARNING = "This method blocks the thread and is not recommended. Use carefully."

open class IonPromise<T>: Promise<T> {
    val affinity: AsyncAffinity?

    constructor(affinity: AsyncAffinity?, deferred: Deferred<T>) : super(deferred) {
        this.affinity = affinity
    }

    constructor(affinity: AsyncAffinity?, promise: Promise<T>): this(affinity, promise.deferred)

    constructor(affinity: AsyncAffinity?, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend() -> T): super(start, block) {
        this.affinity = affinity
    }

    @Deprecated(THREAD_BLOCK_WARNING)
    fun get(): T {
        return PromiseHelper.get(this)
    }

    @Deprecated(THREAD_BLOCK_WARNING)
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
