package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncInput
import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.copy
import com.koushikdutta.scratch.event.AsyncEventLoop
import com.koushikdutta.scratch.http.AsyncHttpMessageContent
import java.io.File

class FileParser(val loop: AsyncEventLoop, val file: File, override val contentType: String = "application/octet-stream") : AsyncParser<File> {
    override fun parse(read: AsyncRead) = Promise {
        val tmp = File(file.absolutePath + ".tmp")
        loop.await()
        tmp.parentFile?.mkdirs()
        val storage = loop.openFile(tmp, true)
        try {
            read.copy(storage)
            storage.close()
        }
        catch (throwable: Throwable) {
            storage.close()
            tmp.delete()
            throw throwable
        }
        tmp.renameTo(file)
        file
    }

    override val type = File::class.java
}


class FileSerializer(val loop: AsyncEventLoop, val contentType: String = "application/octet-stream") : AsyncSerializer<File> {
    override fun write(value: File) = Promise<AsyncHttpMessageContent> {
        val storage = loop.openFile(value)
        val length = storage.size()
        object : AsyncHttpMessageContent, AsyncInput by storage {
            override val contentLength = length
            override val contentType = this@FileSerializer.contentType
            override suspend fun close(throwable: Throwable?) {
                close()
                if (throwable != null)
                    value.delete()
            }
        }
    }
}