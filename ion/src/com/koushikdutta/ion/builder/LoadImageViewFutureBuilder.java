package com.koushikdutta.ion.builder;

import android.widget.ImageView;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.future.ImageViewFuture;

/**
* Created by koush on 5/30/13.
*/
public interface LoadImageViewFutureBuilder {
    /**
     * Perform the request and get the result as a Bitmap, which will then be loaded
     * into the given ImageView
     * @param url
     * @return
     */
    public ImageViewFuture load(String url);

    /**
     * Perform the request and get the result as a Bitmap, which will then be loaded
     * into the given ImageView
     * @param method
     * @param url
     * @return
     */
    public Future<ImageView> load(String method, String url);
}
