package com.koushikdutta.ion.loader

import com.koushikdutta.scratch.http.AsyncHttpMessageContent
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.uri.URI

/**
 * Created by koush on 7/15/13.
 */
interface AsyncHttpRequestFactory {
    fun createAsyncHttpRequest(uri: URI, method: String, headers: Headers?, body: AsyncHttpMessageContent?): AsyncHttpRequest
}