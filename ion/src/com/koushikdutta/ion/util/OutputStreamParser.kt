package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import java.io.OutputStream
import java.lang.reflect.Type

class OutputStreamParser(val outputStream: OutputStream, val close: Boolean, override val contentType: String = "application/octet-stream") : AsyncParser<OutputStream> {
    override fun parse(read: AsyncRead): Promise<OutputStream> {
        return Promise {
            val buffer = ByteBufferList()
            while (read(buffer)) {
                outputStream.write(buffer.readBytes())
            }
            outputStream
        }
        .finally {
            if (close)
                StreamUtility.closeQuietly(outputStream)
        }
    }

    override val type = OutputStream::class.java
}