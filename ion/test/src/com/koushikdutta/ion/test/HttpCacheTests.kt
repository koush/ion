package com.koushikdutta.ion.test

import androidx.test.platform.app.InstrumentationRegistry
import com.koushikdutta.ion.Ion
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.asPromise
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.http.AsyncHttpResponse
import com.koushikdutta.scratch.http.Headers
import com.koushikdutta.scratch.http.StatusCode
import com.koushikdutta.scratch.http.body.Utf8StringBody
import com.koushikdutta.scratch.http.client.AsyncHttpClient
import com.koushikdutta.scratch.http.client.AsyncHttpExecutor
import com.koushikdutta.scratch.http.client.executor.findHttpClientExecutor
import com.koushikdutta.scratch.http.server.AsyncHttpServer
import com.koushikdutta.scratch.listenAsync
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

open class HttpCacheTests {
    fun testHandlerExpecting(expecting: Headers.() -> Unit, callback: AsyncHttpExecutor) {

        val httpServer = AsyncHttpServer(callback)

        val ionId = synchronized(IonTestScope::class.java) {
            ionNumbers++
        }
        val ion = Ion.getInstance(InstrumentationRegistry.getInstrumentation().context, "ion-$ionId")
        ion.configure().httpLoader.httpClient.findHttpClientExecutor(AsyncHttpClient::class)!!.schemeExecutor.unhandled = callback

        ion.configure().loaders.add(0, TestLoader())

        ion.bitmapCache.clear()

        val result: Promise<Unit> = ion.loop.async {
            ion.cache.clear()

            val serverSocket = listen()
            httpServer.listenAsync(serverSocket)

            val data = Ion.with(InstrumentationRegistry.getInstrumentation().context)
                    .load("http://localhost:${serverSocket.localPort}/")
                    .setTimeout(100000000)
                    .asString()
                    .await()

            val dataResponse = Ion.with(InstrumentationRegistry.getInstrumentation().context)
                    .load("http://localhost:${serverSocket.localPort}/")
                    .asString()
                    .withResponse()
                    .await()

            expecting(dataResponse.headers!!.headers)
            val data2 = dataResponse.result.getOrThrow()

            assertEquals(data, data2)
        }
        .asPromise()

        result.catch {
            println(it)
        }

        val semaphore = Semaphore(0)
        result.finally {
            ion.loop.stop()
            semaphore.release()
        }
        semaphore.tryAcquire(100000000, TimeUnit.MILLISECONDS)

        result.getOrThrow()
    }

    fun testHeadersExpecting(callback: AsyncHttpResponse.() -> Unit, expecting: Headers.() -> Unit) = testHandlerExpecting(expecting) {
        val response = StatusCode.OK(body = Utf8StringBody("hello world"))
        callback(response)
        response
    }

    fun testHeadersExpecting(callback: AsyncHttpResponse.() -> Unit) = testHeadersExpecting(callback) {
        assertEquals(this["X-Scratch-Cache"], "Cache")
    }

    fun testHeadersExpectingNotCached(callback: AsyncHttpResponse.() -> Unit) = testHeadersExpecting(callback) {
        assertNull(this["X-Scratch-Cache"])
    }

    @Test
    fun testCacheImmutable() = testHeadersExpecting {
        headers["Cache-Control"] = "immutable"
    }

    @Test
    fun testCacheMaxAge() = testHeadersExpecting {
        headers["Cache-Control"] = "max-age=300"
    }

    @Test
    fun testCacheMaxAgeExpired() = testHeadersExpectingNotCached {
        headers["Cache-Control"] = "max-age=0"
    }

    @Test
    fun testConditionalCache() = testHandlerExpecting({
        assertEquals(this["X-Scratch-Cache"], "ConditionalCache")
    }) {
        if (it.headers["If-None-Match"] == "hello") {
            StatusCode.NOT_MODIFIED()
        }
        else {
            val headers = Headers()
            headers["ETag"] = "hello"
            StatusCode.OK(headers = headers, body = Utf8StringBody("hello world"))
        }
    }

    @Test
    fun testConditionalCacheMismatch() = testHandlerExpecting({
        assertNull(this["X-Scratch-Cache"])
    }) {
        if (it.headers["If-None-Match"] == "world") {
            StatusCode.NOT_MODIFIED()
        }
        else {
            val headers = Headers()
            headers["ETag"] = "hello"
            StatusCode.OK(headers = headers, body = Utf8StringBody("hello world"))
        }
    }
}