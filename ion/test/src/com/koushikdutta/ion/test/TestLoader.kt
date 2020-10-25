package com.koushikdutta.ion.test

import com.koushikdutta.ion.*
import com.koushikdutta.ion.loader.SimpleLoader
import com.koushikdutta.scratch.AsyncReader
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.collections.*
import com.koushikdutta.scratch.http.*
import com.koushikdutta.scratch.http.body.BufferBody
import com.koushikdutta.scratch.http.body.Utf8StringBody
import com.koushikdutta.scratch.http.server.AsyncHttpRouter
import com.koushikdutta.scratch.http.server.get
import com.koushikdutta.scratch.http.server.post
import com.koushikdutta.scratch.parser.*
import org.json.JSONObject

class TestLoader : SimpleLoader() {
    val router = AsyncHttpRouter()

    init {
        router.post("/echo") {
            val data = readAllBuffer(it.body ?: { false })
            StatusCode.OK(body = BufferBody(data.readBytes()))
        }

        router.get("/hello") {
            StatusCode.OK(body = Utf8StringBody("hello"))
        }

        router.get("/query") {
            val json = JSONObject()

            val query = it.parseQuery()
            for (key in query.keys) {
                val value = query.getFirst(key)
                json.put(key, value)
            }

            val headers = Headers()
            headers.contentType = "application/json"
            StatusCode.OK(body = Utf8StringBody(json.toString()), headers = headers)
        }

        router.post("/urlencoded") {
            val json = JSONObject()
            val body = readAllString(it.body!!)
            val map = parseUrlEncoded(body)
            for (key in map.keys) {
                val value = map.getFirst(key)
                json.put(key, value)
            }

            val headers = Headers()
            headers.contentType = "application/json"
            StatusCode.OK(body = Utf8StringBody(json.toString()), headers = headers)
        }

        router.post("/multipart") {
            val json = JSONObject()
            val reader = AsyncReader(it.body!!)
            val boundary = it.headers.multipartBoundary!!
            val multipart = Multipart.parseMultipart(boundary, reader)
            for (part in multipart) {
                val key = part.name!!
                val value = readAllString(part.body)
                json.put(key, value)
            }

            val headers = Headers()
            headers.contentType = "application/json"
            StatusCode.OK(body = Utf8StringBody(json.toString()), headers = headers)
        }
    }

    override fun load(ion: Ion, options: IonRequestOptions, request: AsyncHttpRequest): Promise<Loader.LoaderResult>? {
        if (request.uri.scheme != "test")
            return null

        return Promise {
            val response = router.handle(request)
            Loader.LoaderResult(response, response.headers.contentLength, ResponseServedFrom.LOADED_FROM_MEMORY, HeadersResponse(response), request)
        }
    }
}

