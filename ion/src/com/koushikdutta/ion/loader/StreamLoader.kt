package com.koushikdutta.ion.loader

import android.content.Context
import android.graphics.BitmapFactory
import com.koushikdutta.ion.*
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.ion.util.StreamUtility
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.event.await
import com.koushikdutta.scratch.http.AsyncHttpRequest
import com.koushikdutta.scratch.stream.createAsyncInput
import com.koushikdutta.scratch.uri.URI
import kotlinx.coroutines.Deferred
import java.io.InputStream

/**
 * Created by koush on 6/27/14.
 */
open class StreamLoader : SimpleLoader() {
    @Throws(Exception::class)
    protected open fun getInputStream(ion: Ion, uri: URI): Deferred<Pair<Long?, InputStream>>? {
        return null
    }

    override fun load(ion: Ion, options: IonRequestOptions, request: AsyncHttpRequest): Deferred<Loader.LoaderResult>? {
        val deferredStream = getInputStream(ion, request.uri)
        if (deferredStream == null)
            return null

        return ion.loop.async {
            val data = deferredStream.await()
            val available = data.first
            val stream = data.second
            val input = stream.createAsyncInput()

            Loader.LoaderResult(input, available, ResponseServedFrom.LOADED_FROM_CACHE, null, request)
        }
    }

    override fun loadBitmap(context: Context, ion: Ion, key: String, request: AsyncHttpRequest, resizeWidth: Int, resizeHeight: Int, animateGif: Boolean): Deferred<BitmapInfo>? {
        val deferredStream = getInputStream(ion, request.uri)
        if (deferredStream == null)
            return null
        return ion.loop.async {
            val probe = deferredStream.await()
            val options: BitmapFactory.Options = ion.bitmapCache.prepareBitmapOptions(probe.second, resizeWidth, resizeHeight)
            StreamUtility.closeQuietly(probe.second)

            val stream = getInputStream(ion, request.uri)!!.await().second
            try {
                Ion.getBitmapLoadExecutorService().await()
                BitmapManager.loadBitmap(ion, key, stream, options, animateGif)
            }
            finally {
                StreamUtility.closeQuietly(stream)
            }
        }
    }
}