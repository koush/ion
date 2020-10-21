package com.koushikdutta.ion.util

import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import kotlinx.coroutines.*

class ParserMessageBody<T>(val value: T, val serializer: AsyncSerializer<T>) {
    fun defer(): Deferred<AsyncHttpMessageBody> = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
        serializer.write(value).await()
    }
}
