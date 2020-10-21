package com.koushikdutta.ion

import com.koushikdutta.scratch.collections.Multimap

internal class PendingBitmaps {
    val map: Multimap<String, BitmapPromise> = mutableMapOf()
}