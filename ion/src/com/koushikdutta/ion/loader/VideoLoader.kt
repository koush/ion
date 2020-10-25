package com.koushikdutta.ion.loader

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.os.Build
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.ResponseServedFrom
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.event.await
import com.koushikdutta.scratch.http.AsyncHttpRequest
import kotlinx.coroutines.Deferred
import java.io.File
import java.net.URI

/**
 * Created by koush on 11/6/13.
 */
class VideoLoader : SimpleLoader() {
    override fun loadBitmap(context: Context, ion: Ion, key: String, request: AsyncHttpRequest, resizeWidth: Int, resizeHeight: Int, animateGif: Boolean): Deferred<BitmapInfo>? {
        if (request.uri.scheme != "file")
            return null;

        val type = MediaFile.getFileType(request.uri.toString())
        if (type == null || !MediaFile.isVideoFileType(type.fileType))
            return null

        return ion.loop.async {
            Ion.getBitmapLoadExecutorService().await()
            val file = File(URI.create(request.uri.toString()));
            var bmp = createVideoThumbnail(file.absolutePath)
            val originalSize = Point(bmp.width, bmp.height)
            if (bmp.width > resizeWidth * 2 && bmp.height > resizeHeight * 2) {
                val xratio = resizeWidth.toFloat() / bmp.width
                val yratio = resizeHeight.toFloat() / bmp.height
                val ratio = Math.min(xratio, yratio)
                if (ratio != 0f)
                    bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
            }
            val info = BitmapInfo(key, type.mimeType, bmp, originalSize)
            info.servedFrom = ResponseServedFrom.LOADED_FROM_CACHE
            info
        }
    }

    companion object {
        @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
        @Throws(Exception::class)
        fun createVideoThumbnail(filePath: String?): Bitmap {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            return try {
                retriever.frameAtTime!!
            }
            finally {
                try {
                    retriever.release()
                }
                catch (ignored: Exception) {
                }
            }
        }

    }
}