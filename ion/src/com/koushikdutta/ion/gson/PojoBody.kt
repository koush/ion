package com.koushikdutta.ion.gson

import com.google.gson.Gson
import com.koushikdutta.ion.util.AsyncParser
import com.koushikdutta.ion.util.AsyncSerializer
import com.koushikdutta.ion.util.ByteBufferListParser
import com.koushikdutta.ion.util.ByteBufferListSerializer
import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.buffers.ByteBufferListInputStream
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type

/**
 * Created by koush on 6/5/13.
 */
class PojoParser<T>(val gson: Gson, override val type: Type) : AsyncParser<T> {
    override fun parse(read: AsyncRead): Promise<T> = ByteBufferListParser().parse(read).then {
        gson.fromJson(InputStreamReader(ByteBufferListInputStream(it)), type)
    }

    override val contentType = "application/json"
}

class PojoSerializer<T>(val gson: Gson, val type: Type) : AsyncSerializer<T> {
    override fun write(value: T): Promise<AsyncHttpMessageBody> {
        val bout = ByteArrayOutputStream()
        val out = OutputStreamWriter(bout)
        gson.toJson(value, type, out)
        try {
            out.flush()
            bout.flush()
        }
        catch (e: Exception) {
        }
        val buffer = ByteBufferList()
        buffer.add(bout.toByteArray())
        return ByteBufferListSerializer().write(buffer)
    }
}
