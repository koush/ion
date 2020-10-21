package com.koushikdutta.ion.gson

import com.google.gson.JsonObject
import java.nio.charset.Charset

/**
 * Created by koush on 6/23/14.
 */
class GsonObjectParser : GsonParser<JsonObject> {
    constructor() : super(JsonObject::class.java) {}
    constructor(charset: Charset?) : super(JsonObject::class.java, charset!!)
}

class GsonObjectSerializer : GsonSerializer<JsonObject?>()