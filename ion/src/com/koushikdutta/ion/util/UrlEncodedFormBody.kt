package com.koushikdutta.ion.util

import com.koushikdutta.scratch.collections.StringMultimap
import com.koushikdutta.scratch.collections.add
import com.koushikdutta.scratch.collections.toString
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import kotlinx.coroutines.*

class UrlEncodedFormBody {
    val map: StringMultimap = mutableMapOf()

    fun add(name: String, value: String) {
        map.add(name, value)
    }

    fun defer(): Deferred<AsyncHttpMessageBody> = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
        UrlEncodedFormBodySerializer().write(map).await()
    }
}