package com.koushikdutta.ion.util

import com.koushikdutta.scratch.collections.StringMultimap
import com.koushikdutta.scratch.collections.add
import com.koushikdutta.scratch.collections.getFirst

class ContentDisposition {
    val map: StringMultimap = mutableMapOf()

    var name: String?
        get() = map.getFirst("name")
        set(value) {
            map.add("name", value)
        }

    var filename: String?
        get() = map.getFirst("filename")
        set(value) {
            map.add("filename", value)
        }
}