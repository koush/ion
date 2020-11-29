package com.koushikdutta.ion.util

import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.collections.StringMultimap
import com.koushikdutta.scratch.collections.toString
import com.koushikdutta.scratch.http.AsyncHttpMessageContent

class UrlEncodedFormBodySerializer: AsyncSerializer<StringMultimap> {
    override fun write(value: StringMultimap): Promise<AsyncHttpMessageContent> {
        val string = value.toString("&", ",", false)
        return StringSerializer(contentType = "application/x-www-form-urlencoded").write(string)
    }
}
