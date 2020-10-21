package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBuffer
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import java.nio.charset.Charset

class StringParser(val charset: Charset = Charsets.UTF_8, override val contentType: String = "text/plain") : AsyncParser<String> {
    override fun parse(read: AsyncRead): Promise<String> = ByteBufferListParser().parse(read).then {
        String(it.readBytes(), charset)
    }

    override val type = String::class.java
}

class StringSerializer(val charset: Charset = Charsets.UTF_8, val contentType: String = "text/plain") : AsyncSerializer<String> {
    override fun write(value: String): Promise<AsyncHttpMessageBody> = ByteBufferListSerializer(contentType).write(ByteBufferList(ByteBuffer.wrap(value.toByteArray(charset))))
}
