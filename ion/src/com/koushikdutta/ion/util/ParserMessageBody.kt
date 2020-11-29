package com.koushikdutta.ion.util

import com.koushikdutta.scratch.http.AsyncHttpMessageContent
import kotlinx.coroutines.*

class ParserMessageBody<T>(val value: T, val serializer: AsyncSerializer<T>) {
    fun defer(): Deferred<AsyncHttpMessageContent> = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
        serializer.write(value).await()
    }
}
