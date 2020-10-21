package com.koushikdutta.ion.util

import com.koushikdutta.scratch.collections.StringMultimap
import com.koushikdutta.scratch.collections.add

class QueryString {
    val map: StringMultimap = mutableMapOf()

    fun add(key: String, value: String) {
        map.add(key, value)
    }
}