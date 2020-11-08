package com.koushikdutta.ion.builder

import com.koushikdutta.ion.ContextReference
import com.koushikdutta.ion.IonContext
import com.koushikdutta.scratch.AsyncAffinity
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.PromiseHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine

private const val THREAD_BLOCK_WARNING = "This method blocks the thread and is not recommended. Use carefully."

open class IonPromise<T>: Promise<T> {
    val affinity: AsyncAffinity?
    val contextReference: IonContext?

    constructor(contextReference: IonContext?, affinity: AsyncAffinity?, deferred: Deferred<T>) : super(deferred) {
        this.contextReference = contextReference
        this.affinity = affinity
    }

    constructor(contextReference: IonContext?, affinity: AsyncAffinity?, promise: Promise<T>): this(contextReference, affinity, promise.deferred)

    constructor(contextReference: IonContext?, affinity: AsyncAffinity?, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend() -> T): super(start, block) {
        this.contextReference = contextReference
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

    /**
     * Whether this promise will throw a cancellation exception when cancelled, or end silently.
     * By default, cancellations will be silent.
     */
    var cancelSilently = true

    override suspend fun await(): T {
        try {
            val ret = super.await()
            affinity?.await()
            contextReference?.ensureAlive()
            return ret
        }
        catch (cancelled: CancellationException) {
            if (cancelSilently) {
                suspendCoroutine<Unit> {}
            }
            affinity?.await()
            throw cancelled
        }
        finally {
            affinity?.await()
        }
    }
}
