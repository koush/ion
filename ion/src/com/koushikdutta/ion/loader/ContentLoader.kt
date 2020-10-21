package com.koushikdutta.ion.loader

import android.content.Context
import android.net.Uri
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.uri.URI
import kotlinx.coroutines.Deferred
import java.io.InputStream

/**
 * Created by koush on 5/22/13.
 */
class ContentLoader : StreamLoader() {
    override fun getInputStream(ion: Ion, uri: URI): Deferred<Pair<Long?, InputStream>>? {
        if (uri.scheme != "content")
            return null

        return ion.loop.async {
            val stream = ion.context.contentResolver.openInputStream(Uri.parse(uri.toString()))!!
            val available = stream.available().toLong()
            Pair(available, stream)
        }
    }
}
