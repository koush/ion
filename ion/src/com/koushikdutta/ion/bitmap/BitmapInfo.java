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
    public Bitmap[] bitmaps;
    public int[] delays;

    public int sizeOf() {
        if (bitmaps == null)
            return 0;
        return bitmaps[0].getRowBytes() * bitmaps[0].getHeight() * bitmaps.length;
    }
}
