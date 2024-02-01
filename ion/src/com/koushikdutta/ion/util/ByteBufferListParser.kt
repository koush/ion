package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.createReader
import com.koushikdutta.scratch.http.AsyncHttpMessageContent
import com.koushikdutta.scratch.parser.readBuffer

class ByteBufferListParser(override val contentType: String = "application/octet-stream") : AsyncParser<ByteBufferList> {
    override fun parse(read: AsyncRead) = Promise {
        val parser = com.koushikdutta.scratch.parser.AsyncParser(read)
        parser.readBuffer()
    }

    override val type = ByteBufferList::class.java
}

class ByteBufferListSerializer(val contentType: String = "application/octet-stream") : AsyncSerializer<ByteBufferList> {
    override fun write(value: ByteBufferList): Promise<AsyncHttpMessageContent> {
        return Promise.resolve(object : AsyncHttpMessageContent, AsyncRead by value.createReader() {
            override val contentLength = value.remaining().toLong()
            override val contentType = this@ByteBufferListSerializer.contentType
            override suspend fun close() {}
        })
    }
}
