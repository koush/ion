package com.koushikdutta.ion.loader

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.ResponseServedFrom
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.event.await
import com.koushikdutta.scratch.http.AsyncHttpRequest
import kotlinx.coroutines.Deferred

/**
 * Created by koush on 11/3/13.
 */
class PackageIconLoader : SimpleLoader() {
    override fun loadBitmap(context: Context, ion: Ion, key: String, request: AsyncHttpRequest, resizeWidth: Int, resizeHeight: Int, animateGif: Boolean): Deferred<BitmapInfo>? {
        if (request.uri.scheme != "package")
            return null

        return ion.loop.async {
            Ion.getBitmapLoadExecutorService().await()

            val pkg = request.uri.authority!!
            val pm = ion.context.packageManager
            val bmp = (pm.getPackageInfo(pkg, 0).applicationInfo.loadIcon(pm) as BitmapDrawable).bitmap
                    ?: throw Exception("package icon failed to load")
            val info = BitmapInfo(key, null, bmp, Point(bmp.width, bmp.height))
            info.servedFrom = ResponseServedFrom.LOADED_FROM_CACHE
            info
        }
    }
}
