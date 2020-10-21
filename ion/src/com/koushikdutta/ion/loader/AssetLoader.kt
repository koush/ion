package com.koushikdutta.ion.loader

import android.net.Uri
import com.koushikdutta.ion.Ion
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.uri.URI
import kotlinx.coroutines.Deferred
import java.io.InputStream

/**
 * Created by koush on 6/27/14.
 */
class AssetLoader : StreamLoader() {
    @Override
    override fun getInputStream(ion: Ion, uri: URI): Deferred<Pair<Long, InputStream>>? {
        if (!uri.toString().startsWith("file:///android_asset/"))
            return null
        return ion.loop.async {
            val stream = ion.context.assets.open(Uri.parse(uri.toString()).path!!.replaceFirst("^/android_asset/".toRegex(), ""))
            val available = stream.available().toLong()
            Pair(available, stream)
        }
    }
}