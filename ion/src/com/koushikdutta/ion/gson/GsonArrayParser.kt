package com.koushikdutta.ion.gson

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.nio.charset.Charset

/**
 * Created by koush on 6/23/14.
 */
class GsonArrayParser : GsonParser<JsonArray> {
    constructor() : super(JsonArray::class.java) {}
    constructor(charset: Charset?) : super(JsonArray::class.java, charset!!) {}
}

class GsonArraySerializer : GsonSerializer<JsonArray?>()