package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.copy
import com.koushikdutta.scratch.event.AsyncEventLoop
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import java.io.File

class FileParser(val loop: AsyncEventLoop, val file: File, override val contentType: String = "application/octet-stream") : AsyncParser<File> {
    override fun parse(read: AsyncRead) = Promise {
        val storage = loop.openFile(file, true)
        read.copy(storage::write)
        storage.close()
        file
    }

    override val type = File::class.java
}


class FileSerializer(val loop: AsyncEventLoop, val contentType: String = "application/octet-stream") : AsyncSerializer<File> {
    override fun write(value: File) = Promise<AsyncHttpMessageBody> {
        val tmp = loop.openFile(File(value, ".tmp"))
        val storage = loop.openFile(value)
        val length = storage.size()
        val read = storage::read
        object : AsyncHttpMessageBody {
            override val contentLength = length
            override val contentType = this@FileSerializer.contentType
            override val read = read
            override suspend fun sent(throwable: Throwable?) {
                storage.close()
                if (throwable != null)
                    value.delete()
            }
        }
    }
}
