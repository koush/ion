package com.koushikdutta.ion

import android.net.Uri
import android.text.TextUtils
import com.koushikdutta.ion.loader.AsyncHttpRequestFactory
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.uri.URI.Companion.create

class DefaultAsyncHttpRequestFactory : AsyncHttpRequestFactory {
    override fun createAsyncHttpRequest(uri: Uri, method: String, headers: Headers?, body: AsyncHttpMessageBody?): AsyncHttpRequest {
        return AsyncHttpRequest(create(uri.toString()), method, "HTTP/1.1", headers ?: Headers(), body, null)
    }

}