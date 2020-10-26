package com.koushikdutta.ion.loader

import com.koushikdutta.ion.HeadersResponse
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.IonRequestOptions
import com.koushikdutta.ion.Loader.LoaderResult
import com.koushikdutta.ion.ResponseServedFrom
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.client.AsyncHttpClient
import com.koushikdutta.scratch.http.client.buildUpon
import com.koushikdutta.scratch.http.client.executor.CacheResult
import com.koushikdutta.scratch.http.client.executor.cacheResult
import com.koushikdutta.scratch.http.client.executor.useCache
import com.koushikdutta.scratch.http.client.followRedirects
import com.koushikdutta.scratch.http.contentLength
import kotlinx.coroutines.Deferred

/**
 * Created by koush on 5/22/13.
 */
class HttpLoader(ion: Ion) : SimpleLoader() {
    override fun load(ion: Ion, options: IonRequestOptions, request: AsyncHttpRequest): Deferred<LoaderResult>? {
        val scheme = request.uri.scheme
        if (scheme != "http" && scheme != "https")
            return null
        return ion.loop.async {
            val client = if (options.followRedirect)
                httpClient.buildUpon().followRedirects().build()
            else
                httpClient

            if (options.noCache && !request.headers.contains("Cache-Control"))
                request.headers["Cache-Control"] = "no-store"

            val response = client(request)

            val responseServedFrom = when(response.cacheResult) {
                CacheResult.Cache -> ResponseServedFrom.LOADED_FROM_CACHE
                CacheResult.ConditionalCache -> ResponseServedFrom.LOADED_FROM_CONDITIONAL_CACHE
                else -> ResponseServedFrom.LOADED_FROM_NETWORK
            }

            LoaderResult(response, response.headers.contentLength,
                    // fixup cache reporting
                    responseServedFrom,
                    HeadersResponse(response.code, response.message, response.headers),
                    // fixup redirected final request
                    request)
        }
    }

    val httpClient = AsyncHttpClient(ion.loop)
            .buildUpon()
            .useCache(ion.cache)
            .build()
}
