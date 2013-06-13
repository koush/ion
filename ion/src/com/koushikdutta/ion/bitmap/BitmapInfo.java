package com.koushikdutta.ion.bitmap;

import android.graphics.Bitmap;

/**
 * Created by koush on 6/12/13.
 */
public class BitmapInfo {
    public long loadTime = System.currentTimeMillis();
    public long drawTime;
    public String key;
    public int loadedFrom;
    public Bitmap bitmap;
}
