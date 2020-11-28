package com.koushikdutta.ion

import android.app.ProgressDialog
import android.widget.ProgressBar
import com.koushikdutta.ion.builder.ResponsePromise
import com.koushikdutta.ion.util.AsyncParser
import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.event.createScheduler
import com.koushikdutta.scratch.event.monitor
import com.koushikdutta.scratch.event.timeout
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.http.client.executor.setProxy
import com.koushikdutta.scratch.http.contentLength
import com.koushikdutta.scratch.http.contentType
import com.koushikdutta.scratch.uri.URI
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

interface IonRequestOptions {
    val followRedirect: Boolean
    val noCache: Boolean
}

private fun AsyncRead.observe(progressCallback: (Long) -> Unit): AsyncRead {
    var total = 0L
    return AsyncRead {
        val before = it.remaining()
        val ret = this(it)
        val progress = it.remaining() - before
        if (progress != 0) {
            total += progress
            progressCallback(total)
        }
        ret
    }
}

internal class IonExecutor<T>(ionRequestBuilder: IonRequestBuilder, val parser: AsyncParser<T>, val cancel: Runnable?): IonRequestOptions {
    val ion = ionRequestBuilder.ion
    val contextReference = ionRequestBuilder.contextReference
    val headers = ionRequestBuilder.headers ?: Headers()
    val rawRequest = ionRequestBuilder.rawRequest
    val handler = ionRequestBuilder.handler
    val query = ionRequestBuilder.query
    val uri = ionRequestBuilder.uri
    val method = ionRequestBuilder.method
    val body = ionRequestBuilder.body
    val proxyHost = ionRequestBuilder.proxyHost
    val proxyPort = ionRequestBuilder.proxyPort
    val timeoutMilliseconds = ionRequestBuilder.timeoutMilliseconds
    val affinity = handler?.createScheduler()
    override val followRedirect = ionRequestBuilder.followRedirect
    val progressBar = ionRequestBuilder.progressBar
    val progressDialog = ionRequestBuilder.progressDialog
    val progress = ionRequestBuilder.progress
    val progressHandler = ionRequestBuilder.progressHandler
    val uploadProgressBar = ionRequestBuilder.uploadProgressBar
    val uploadProgressDialog = ionRequestBuilder.uploadProgressDialog
    val uploadProgress = ionRequestBuilder.uploadProgress
    val uploadProgressHandler = ionRequestBuilder.uploadProgressHandler
    override val noCache = ionRequestBuilder.noCache

    suspend fun loadRequest(request: AsyncHttpRequest): Loader.LoaderResult {
        // now attempt to fetch it directly
        for (loader in ion.loaders) {
            val emitter = loader.load(ion, this, request)
            if (emitter != null)
                return emitter.await()
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
        return ion.configure().getAsyncHttpRequestFactory().createAsyncHttpRequest(URI(uri.toString()), method, headers, body)
    }

    suspend fun prepareRequest(): AsyncHttpRequest {
        val request = rawRequest ?: prepareURI()
        if (proxyHost != null)
            request.setProxy(proxyHost, proxyPort)
        return setupUploadProgress(request)
    }

    fun setupUploadProgress(request: AsyncHttpRequest): AsyncHttpRequest {
        val body = request.body ?: return request
        val read = setupProgress(body, request.headers.contentLength, uploadProgress, uploadProgressBar, uploadProgressDialog, uploadProgressHandler)
        if (read == null)
            return request
        return AsyncHttpRequest(request.requestLine, request.headers, read, request::close)
    }

    companion object {
        fun setupProgress(read: AsyncRead, length: Long?, progress: ProgressCallback?, progressBar: WeakReference<ProgressBar>?, progressDialog: WeakReference<ProgressDialog>?, progressHandler: ProgressCallback?): AsyncRead? {
            if (progress == null && progressBar == null && progressDialog == null && progressHandler == null)
                return null

            Ion.mainHandler.post {
                if (length == null) {
                    progressBar?.get()?.isIndeterminate = true
                    progressDialog?.get()?.isIndeterminate = true
                }
                else {
                    progressBar?.get()?.max = length.toInt()
                    progressDialog?.get()?.max = length.toInt()
                }
            }

            return read.observe {
                if (progressBar != null || progressDialog != null) {
                    Ion.mainHandler.post {
                        progressBar?.get()?.setProgress(it.toInt())
                        progressDialog?.get()?.setProgress(it.toInt())
                        progressHandler?.onProgress(it, length)
                    }
                }
                progress?.onProgress(it, length)
            }
        }
    }


    fun setupDownloadProgress(emitter: Loader.LoaderResult): AsyncRead = setupProgress(emitter.input, emitter.length, progress, progressBar, progressDialog, progressHandler) ?: emitter.input

    fun <F> executeParser(parser: AsyncParser<F>, fastLoad: suspend(request: AsyncHttpRequest) -> Response<F>? = { null }): ResponsePromise<F> {
        val response: Deferred<Response<F>> = GlobalScope.async(Dispatchers.Unconfined) {
            val finalRequest = resolvedRequest.await()

            // should this be guarded in a timeout?
            fastLoad(finalRequest)?.let {
                return@async it
            }

            val emitter = ion.loop.timeout(timeoutMilliseconds.toLong()) {
                ion.loop.monitor(1000L, { contextReference.isAlive == null }) {
                    loadRequest(finalRequest)
                }
            }

            val response: Response<F> = try {
                val read = setupDownloadProgress(emitter)
                val value = parser.parse(read).await()
                Response(emitter.resolvedRequest, emitter.servedFrom, emitter.headers, com.koushikdutta.scratch.Result.success(value))
            }
            catch (throwable: Throwable) {
                Response(emitter.resolvedRequest, emitter.servedFrom, emitter.headers, com.koushikdutta.scratch.Result.failure(throwable))
            }
            finally {
                emitter.input.close()
            }

            println(response)
            response
        }

        return ResponsePromise(affinity, response)
    }

    fun execute(): ResponsePromise<T> {
        return executeParser(parser) {
            val type = parser.type
            if (type != null) {
                for (loader in ion.loaders) {
                    loader?.load(ion, it, type)?.let { fastLoad ->
                        return@executeParser fastLoad.await()
                    }
                }
            }
            null
        }
    }
}
