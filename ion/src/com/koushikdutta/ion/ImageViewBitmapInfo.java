package com.koushikdutta.ion;

import android.widget.ImageView;

import com.koushikdutta.ion.bitmap.BitmapInfo;

/**
 * Created by koush on 7/1/14.
 */
public class ImageViewBitmapInfo {
    Throwable exception;
    public Throwable getException() {
        return exception;
    }

    ImageView imageView;
    public ImageView getImageView() {
        return imageView;
    }

    BitmapInfo info;
    public BitmapInfo getBitmapInfo() {
        return info;
    }
}
