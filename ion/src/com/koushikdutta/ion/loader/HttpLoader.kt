package com.koushikdutta.ion.loader

import com.koushikdutta.ion.HeadersResponse
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.IonRequestOptions
import com.koushikdutta.ion.Loader.LoaderResult
import com.koushikdutta.ion.ResponseServedFrom
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.event.AsyncEventLoop
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.client.AsyncHttpClient
import com.koushikdutta.scratch.http.client.buildUpon
import com.koushikdutta.scratch.http.client.followRedirects
import com.koushikdutta.scratch.http.contentLength
import kotlinx.coroutines.Deferred

/**
 * Created by koush on 5/22/13.
 */
class HttpLoader(val loop: AsyncEventLoop) : SimpleLoader() {
    override fun load(ion: Ion, options: IonRequestOptions, request: AsyncHttpRequest): Deferred<LoaderResult>? {
        val scheme = request.uri.scheme
        if (scheme != "http" && scheme != "https")
            return null
        return loop.async {
            val client = if (options.followRedirect)
                httpClient.buildUpon().followRedirects().build()
            else
                httpClient
            val response = client(request)

            LoaderResult(response, response.headers.contentLength,
                    // fixup cache reporting
                    ResponseServedFrom.LOADED_FROM_NETWORK,
                    HeadersResponse(response.code, response.message, response.headers),
                    // fixup redirected final request
                    request)
        }
    }

    val httpClient = AsyncHttpClient(loop)
}