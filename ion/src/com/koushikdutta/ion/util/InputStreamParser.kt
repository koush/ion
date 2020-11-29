package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncInput
import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferListInputStream
import com.koushikdutta.scratch.http.AsyncHttpMessageContent
import com.koushikdutta.scratch.stream.createAsyncInput
import java.io.InputStream

class InputStreamParser(override val contentType: String = "application/octet-stream") : AsyncParser<InputStream> {
    override fun parse(read: AsyncRead): Promise<InputStream> = ByteBufferListParser().parse(read).then {
        ByteBufferListInputStream(it)
    }

    override val type = InputStream::class.java
}

class InputStreamSerializer(val contentLength: Long? = null, val contentType: String = "application/octet-stream") : AsyncSerializer<InputStream> {
    override fun write(value: InputStream) = Promise<AsyncHttpMessageContent> {
        val input = value.createAsyncInput()
        object : AsyncHttpMessageContent, AsyncInput by input {
            override val contentLength = this@InputStreamSerializer.contentLength
            override val contentType = this@InputStreamSerializer.contentType
        }
    }
}
