package com.koushikdutta.ion

import com.koushikdutta.ion.loader.AsyncHttpRequestFactory
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.uri.URI

class DefaultAsyncHttpRequestFactory : AsyncHttpRequestFactory {
    override fun createAsyncHttpRequest(uri: URI, method: String, headers: Headers?, body: AsyncHttpMessageBody?): AsyncHttpRequest {
        return AsyncHttpRequest(uri, method, "HTTP/1.1", headers ?: Headers(), body, null)
    }
}
