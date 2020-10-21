package com.koushikdutta.ion.loader

import android.net.Uri
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.Headers

/**
 * Created by koush on 7/15/13.
 */
interface AsyncHttpRequestFactory {
    fun createAsyncHttpRequest(uri: Uri, method: String, headers: Headers?, body: AsyncHttpMessageBody?): AsyncHttpRequest
}