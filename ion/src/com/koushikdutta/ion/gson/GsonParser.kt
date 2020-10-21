package com.koushikdutta.ion.gson

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.koushikdutta.ion.util.AsyncParser
import com.koushikdutta.ion.util.AsyncSerializer
import com.koushikdutta.ion.util.ByteBufferListParser
import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.buffers.ByteBufferListInputStream
import com.koushikdutta.scratch.createReader
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.nio.charset.Charset

/**
 * Created by koush on 5/27/13.
 */
abstract class GsonParser<T : JsonElement?>(private val clazz: Class<out T>, private val charset: Charset = Charsets.UTF_8) : AsyncParser<T> {
    override fun parse(read: AsyncRead) = ByteBufferListParser().parse(read).then {
        val bis = ByteBufferListInputStream(it)
        val isr = InputStreamReader(bis, charset)
        val parsed = JsonParser.parseReader(JsonReader(isr))
        if (parsed.isJsonNull || parsed.isJsonPrimitive) throw JsonParseException("unable to parse json")
        if (!clazz.isInstance(parsed)) throw ClassCastException(parsed.javaClass.canonicalName + " can not be casted to " + clazz.canonicalName)
        parsed as T
    }

    override val contentType = "application/json"
    override val type = clazz
}

abstract class GsonSerializer<T : JsonElement?>(private val charset: Charset = Charsets.UTF_8) : AsyncSerializer<T> {
    override fun write(value: T) = Promise<AsyncHttpMessageBody> {
        val buffer = ByteBufferList()
        buffer.add(value.toString().toByteArray(charset))
        object : AsyncHttpMessageBody {
            override val contentType = "application/json"
            override val contentLength = buffer.remaining().toLong()
            override val read = buffer.createReader()
        }
    }
}
