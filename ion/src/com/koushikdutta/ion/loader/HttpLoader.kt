package com.koushikdutta.ion.loader

import com.koushikdutta.ion.HeadersResponse
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.Loader.LoaderResult
import com.koushikdutta.ion.ResponseServedFrom
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.asPromise
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.event.AsyncEventLoop
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.client.AsyncHttpClient
import com.koushikdutta.scratch.http.contentLength

/**
 * Created by koush on 5/22/13.
 */
class HttpLoader(val loop: AsyncEventLoop) : SimpleLoader() {
    override fun load(ion: Ion, request: AsyncHttpRequest): Promise<LoaderResult>? {
        val scheme = request.uri.scheme
        if (scheme != "http" && scheme != "https")
            return null
        return loop.async {
            val response = httpClient(request)

            LoaderResult(response, response.headers.contentLength,
                    // fixup cache reporting
                    ResponseServedFrom.LOADED_FROM_NETWORK,
                    HeadersResponse(response.code, response.message, response.headers),
                    // fixup redirected final request
                    request)
        }
        .asPromise()
    }

    val httpClient = AsyncHttpClient(loop)
}