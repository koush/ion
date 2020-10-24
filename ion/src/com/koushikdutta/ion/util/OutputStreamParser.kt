package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import java.io.OutputStream
import java.lang.reflect.Type

class OutputStreamParser<F: OutputStream>(val outputStream: F, val close: Boolean, override val contentType: String = "application/octet-stream") : AsyncParser<F> {
    override fun parse(read: AsyncRead): Promise<F> {
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

    override val type: Type = OutputStream::class.java
}