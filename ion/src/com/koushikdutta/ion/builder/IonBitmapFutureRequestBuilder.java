package com.koushikdutta.ion.builder;

import android.graphics.Bitmap;

import com.koushikdutta.async.future.Future;

/**
* Created by koush on 5/30/13.
*/
public interface IonBitmapFutureRequestBuilder {
    /**
     * Perform the request and get the result as a Bitmap
     * @return
     */
    public Future<Bitmap> asBitmap();
}
