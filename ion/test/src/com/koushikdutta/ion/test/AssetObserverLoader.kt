package com.koushikdutta.ion.test

import android.content.Context
import android.net.Uri
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.IonRequestOptions
import com.koushikdutta.ion.Loader
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.ion.loader.StreamLoader
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.uri.URI
import kotlinx.coroutines.Deferred
import java.io.InputStream

class AssetObserverLoader: StreamLoader() {
    var loadObserved = 0
    var loadBitmapObserved = 0

    override fun load(ion: Ion, options: IonRequestOptions, request: AsyncHttpRequest): Deferred<Loader.LoaderResult>? {
        val ret = super.load(ion, options, request)
        if (ret != null)
            loadObserved++
        return ret
    }

    override fun loadBitmap(context: Context, ion: Ion, key: String, request: AsyncHttpRequest, resizeWidth: Int, resizeHeight: Int, animateGif: Boolean): Deferred<BitmapInfo>? {
        if (!request.headers.contains("X-Allow-Bitmap"))
            return null

        val ret = super.loadBitmap(context, ion, key, request, resizeWidth, resizeHeight, animateGif)
        if (ret != null)
            loadBitmapObserved++
        return ret
    }

    override fun getInputStream(ion: Ion, uri: URI): Deferred<Pair<Long?, InputStream>>? {
        if (!uri.toString().startsWith("file:///android_asset_observe/"))
            return null
        return ion.loop.async {
            val stream = ion.context.assets.open(Uri.parse(uri.toString()).path!!.replaceFirst("^/android_asset_observe/".toRegex(), ""))
            val available = stream.available().toLong()
            Pair(available, stream)
        }
    }
}
