package com.koushikdutta.ion;

import android.graphics.Bitmap;

import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.ion.bitmap.BitmapInfo;

class BitmapInfoToBitmap extends TransformFuture<Bitmap, BitmapInfo> {
    ContextReference contextReference;
    public BitmapInfoToBitmap(ContextReference contextReference) {
        this.contextReference = contextReference;
    }

    @Override
    protected void transform(BitmapInfo result) throws Exception {
        if (contextReference.isAlive() != null) {
            cancel();
            return;
        }

        if (result.exception != null)
            setComplete(result.exception);
        else
            setComplete(result.bitmap);
    }
}