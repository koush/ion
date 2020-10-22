package com.koushikdutta.ion

import com.koushikdutta.ion.builder.ResponsePromise
import com.koushikdutta.ion.util.AsyncParser
import com.koushikdutta.scratch.asPromise
import com.koushikdutta.scratch.createAsyncAffinity
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.http.contentType
import kotlinx.coroutines.*
import java.lang.Runnable


internal class IonExecutor<T>(ionRequestBuilder: IonRequestBuilder, val parser: AsyncParser<T>, val cancel: Runnable?) {
    val ion = ionRequestBuilder.ion
    val contextReference = ionRequestBuilder.contextReference
    val headers = ionRequestBuilder.headers ?: Headers()
    val rawRequest = ionRequestBuilder.rawRequest
    val handler = ionRequestBuilder.handler
    val query = ionRequestBuilder.query
    val uri = ionRequestBuilder.uri
    val method = ionRequestBuilder.method
    val body = ionRequestBuilder.body
    val affinity = handler?.createAsyncAffinity()

    suspend fun loadRequest(request: AsyncHttpRequest): Loader.LoaderResult {
        // now attempt to fetch it directly
        for (loader in ion.loaders) {
            val emitter = loader.load(ion, request)
            if (emitter != null) {
//                request.logi("Using loader: $loader")
//                ret.setParent(emitter)
                return emitter.await()
            }
        }

        throw Exception("Unknown uri scheme")
    }

    val resolvedRequest = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
        var current = prepareRequest()
        while (true) {
            var next: AsyncHttpRequest? = null
            // first attempt to resolve the url
            for (loader in ion.loaders) {
                val resolved = loader.resolve(contextReference.context, ion, current)
                if (resolved != null) {
                    next = resolved.await()
                    break
                }
            }
            if (next == null)
                return@async current
            current = next
        }
        current
    }

    suspend fun prepareURI(): AsyncHttpRequest {
        val uri: android.net.Uri
        val query = query?.map
        if (query != null) {
            var builder = android.net.Uri.parse(this.uri).buildUpon()
            for (key in query.keys) {
                for (value in query[key]!!) {
                    builder = builder.appendQueryParameter(key, value)
                }
            }
            uri = builder.build()
        }
        else {
            uri = android.net.Uri.parse(this.uri)
        }

        val body = this.body?.await()
        if (body != null) {
            val ct = body.contentType
            if (ct != null)
                headers.contentType = ct
        }
        return ion.configure().getAsyncHttpRequestFactory().createAsyncHttpRequest(uri, method, headers, body)
    }

    suspend fun prepareRequest(): AsyncHttpRequest {
        return rawRequest ?: prepareURI()
    }

    fun execute(): ResponsePromise<T> {
        val response: kotlinx.coroutines.Deferred<Response<T>> = GlobalScope.async(Dispatchers.Unconfined) {
            val finalRequest = resolvedRequest.await()
            val emitter = loadRequest(finalRequest)
            val response: Response<T> = try {
                val value = parser.parse(emitter.input::read).await()
                Response(emitter.resolvedRequest, emitter.servedFrom, emitter.headers, com.koushikdutta.scratch.Result.success(value))
            }
            catch (throwable: Throwable) {
                Response(emitter.resolvedRequest, emitter.servedFrom, emitter.headers, com.koushikdutta.scratch.Result.failure(throwable))
            }
            finally {
                emitter.input.close()
            }

            affinity?.await()
            response
        }

        val result = GlobalScope.async(Dispatchers.Unconfined) {
            response.await().result.getOrThrow()
        }

        return ResponsePromise(result, affinity, response.asPromise())
    }
}
