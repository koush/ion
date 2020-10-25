package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList

class ByteArrayParser(override val contentType: String = "application/octet-stream") : AsyncParser<ByteArray> {
    override fun parse(read: AsyncRead): Promise<ByteArray> {
        return ByteBufferListParser().parse(read).apply { byteBufferList: ByteBufferList -> byteBufferList.readBytes(byteBufferList.remaining()) }
    }

    override val type = ByteArray::class.java
}
