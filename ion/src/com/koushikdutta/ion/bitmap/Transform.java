package com.koushikdutta.ion.bitmap;

import android.graphics.Bitmap;

/**
 * Created by koush on 5/23/13.
 */
public interface Transform {
    public Bitmap transform(Bitmap b);
    public String key();
}
