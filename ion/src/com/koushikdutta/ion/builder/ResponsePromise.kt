package com.koushikdutta.ion.builder

import com.koushikdutta.ion.Response
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.PromiseHelper
import kotlinx.coroutines.Deferred
import java.util.concurrent.TimeUnit

/**
 * Created by koush on 7/2/13.
 */
class ResponsePromise<T>(wrappedDeferred: Deferred<T>, private val response: Promise<Response<T>>) : Promise<T>(wrappedDeferred) {
    fun withResponse(): Promise<Response<T>> {
        return response
    }

    @Deprecated("This method blocks the thread and is not recommended. Use carefully.")
    fun get(): T {
        return PromiseHelper.get(this)
    }

    @Deprecated("This method blocks the thread and is not recommended. Use carefully.")
    fun get(time: Long, timeUnit: TimeUnit): T {
        return PromiseHelper.get(this, time, timeUnit)
    }
}
