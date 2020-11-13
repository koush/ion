package com.koushikdutta.ion.util

import com.koushikdutta.scratch.asyncIterator
import com.koushikdutta.scratch.collections.toString
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.http.body.Multipart
import com.koushikdutta.scratch.http.body.Part
import kotlinx.coroutines.*

class MultipartBody {
    val parts = arrayListOf<Deferred<Part>>()

    fun addPart(part: Part) {
        val deferred = CompletableDeferred(part)
        parts.add(deferred);
    }

    fun addPart(contentDisposition: ContentDisposition, body: Deferred<AsyncHttpMessageBody>) {
        val part = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
            val headers = Headers()
            headers["Content-Disposition"] = contentDisposition.map.toString(";")
            Part(headers, body = body.await())
        }
        parts.add(part)
    }

    val deferred: Deferred<AsyncHttpMessageBody> = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
        Multipart(asyncIterator {
            for (part in parts) {
                yield(part.await())
            }
        })
    }
}