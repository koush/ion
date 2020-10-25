package com.koushikdutta.ion.util

import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.collections.StringMultimap
import com.koushikdutta.scratch.collections.toString
import com.koushikdutta.scratch.http.AsyncHttpMessageBody

class UrlEncodedFormBodySerializer: AsyncSerializer<StringMultimap> {
    override fun write(value: StringMultimap): Promise<AsyncHttpMessageBody> {
        val string = value.toString("&", ",", false)
        return StringSerializer(contentType = "application/x-www-form-urlencoded").write(string)
    }
}
