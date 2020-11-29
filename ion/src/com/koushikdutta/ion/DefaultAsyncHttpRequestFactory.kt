package com.koushikdutta.ion

import com.koushikdutta.ion.loader.AsyncHttpRequestFactory
import com.koushikdutta.scratch.http.AsyncHttpMessageContent
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.uri.URI

class DefaultAsyncHttpRequestFactory : AsyncHttpRequestFactory {
    override fun createAsyncHttpRequest(uri: URI, method: String, headers: Headers?, body: AsyncHttpMessageContent?): AsyncHttpRequest {
        return AsyncHttpRequest(uri, method, "HTTP/1.1", headers ?: Headers(), body) {
            body?.close()
        }
    }
}
