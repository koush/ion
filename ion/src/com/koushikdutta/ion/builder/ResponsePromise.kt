package com.koushikdutta.ion.builder

import com.koushikdutta.ion.Response
import com.koushikdutta.scratch.AsyncAffinity
import kotlinx.coroutines.Deferred

/**
 * Created by koush on 7/2/13.
 */
class ResponsePromise<T>(affinity: AsyncAffinity?, private val response: Deferred<Response<T>>) : IonPromise<T>(null, affinity, block = { response.await().result.getOrThrow() }) {
    fun withResponse(): IonPromise<Response<T>> {
        return IonPromise(contextReference, affinity, response)
    }
}
