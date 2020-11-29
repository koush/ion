package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.http.AsyncHttpMessageContent
import java.lang.reflect.Type

/**
 * Created by koush on 5/27/13.
 */
interface AsyncSerializer<T> {
    fun write(value: T): Promise<AsyncHttpMessageContent>
}

interface AsyncParser<T> {
    fun parse(read: AsyncRead): Promise<T>
    val contentType: String
    val type: Class<T>?
}
