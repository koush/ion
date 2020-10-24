package com.koushikdutta.ion.builder

import com.koushikdutta.ion.Response
import com.koushikdutta.scratch.AsyncAffinity
import kotlinx.coroutines.Deferred

/**
 * Created by koush on 7/2/13.
 */
class ResponsePromise<T>(wrappedDeferred: Deferred<T>, affinity: AsyncAffinity?, private val response: Deferred<Response<T>>) : IonPromise<T>(affinity, wrappedDeferred) {
    fun withResponse(): IonPromise<Response<T>> {
        return IonPromise(affinity, response)
    }
}
