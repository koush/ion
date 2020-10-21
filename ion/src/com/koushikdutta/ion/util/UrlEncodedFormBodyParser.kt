package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.collections.StringMultimap
import com.koushikdutta.scratch.collections.parseQuery
import com.koushikdutta.scratch.collections.toString
import com.koushikdutta.scratch.http.AsyncHttpMessageBody

class UrlEncodedFormBodyParser: AsyncParser<StringMultimap> {
    override fun parse(read: AsyncRead): Promise<StringMultimap> = StringParser().parse(read).then {
        parseQuery(it)
    }

    override val contentType = "application/x-www-form-urlencoded"
    override val type = UrlEncodedFormBody::class.java
}

class UrlEncodedFormBodySerializer: AsyncSerializer<StringMultimap> {
    override fun write(value: StringMultimap): Promise<AsyncHttpMessageBody> {
        val string = value.toString("&", ",", false)
        return StringSerializer(contentType = "application/x-www-form-urlencoded").write(string)
    }
}
