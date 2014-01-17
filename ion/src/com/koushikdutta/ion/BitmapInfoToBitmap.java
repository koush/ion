package com.koushikdutta.ion;

import android.content.Context;
import android.graphics.Bitmap;

import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.lang.ref.WeakReference;

class BitmapInfoToBitmap extends TransformFuture<Bitmap, BitmapInfo> {
    WeakReference<Context> context;
    public BitmapInfoToBitmap(WeakReference<Context> context) {
        this.context = context;
    }

    @Override
    protected void transform(BitmapInfo result) throws Exception {
        if (!IonRequestBuilder.checkContext(context)) {
            cancel();
            return;
        }

        if (result.exception != null)
            setComplete(result.exception);
        else
            setComplete(result.bitmaps[0]);
    }
}