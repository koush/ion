package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.createReader
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import com.koushikdutta.scratch.parser.readAllBuffer
import java.lang.reflect.Type

class ByteBufferListParser(override val contentType: String = "application/octet-stream") : AsyncParser<ByteBufferList> {
    override fun parse(read: AsyncRead) = Promise {
        readAllBuffer(read)
    }

    override val type = ByteBufferList::class.java
}

class ByteBufferListSerializer(val contentType: String = "application/octet-stream") : AsyncSerializer<ByteBufferList> {
    override fun write(value: ByteBufferList): Promise<AsyncHttpMessageBody> {
        return Promise.resolve(object : AsyncHttpMessageBody {
            override val contentLength = value.remaining().toLong()
            override val contentType = this@ByteBufferListSerializer.contentType
            override val read = value.createReader()
        })
    }
}