package com.koushikdutta.ion

import android.widget.ImageView
import com.koushikdutta.scratch.Promise

/**
 * Created by koush on 7/1/14.
 */
open class ImageViewFuture(private val imageView: ContextReference.ImageViewContextReference, private val bitmapInfo: Promise<ImageViewBitmapInfo>) : Promise<ImageView?>(block = {
    val info = bitmapInfo.await()
    try {
        info.imageView
    }
    catch (throwable: Throwable) {
        imageView.get()
    }
}) {

    open fun withBitmapInfo(): Promise<ImageViewBitmapInfo> {
        return bitmapInfo
    }

    companion object {
        @JvmStatic
        internal fun applyScaleMode(imageView: ImageView, scaleMode: ScaleMode?) {
            if (scaleMode == null) return
            when (scaleMode) {
                ScaleMode.CenterCrop -> imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                ScaleMode.FitCenter -> imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                ScaleMode.CenterInside -> imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                ScaleMode.FitXY -> imageView.scaleType = ImageView.ScaleType.FIT_XY
            }
        }
    }
}
