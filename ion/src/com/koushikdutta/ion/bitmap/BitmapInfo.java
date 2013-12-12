package com.koushikdutta.ion.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;

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
    public Exception exception;

    @TargetApi(19)
    public int sizeOf() {
        if (bitmaps == null)
            return 0;
        if (Build.VERSION.SDK_INT == 19)
            return bitmaps[0].getAllocationByteCount() * bitmaps.length;
        else if (Build.VERSION.SDK_INT == 12)
            return bitmaps[0].getByteCount() * bitmaps.length;
        return bitmaps[0].getRowBytes() * bitmaps[0].getHeight() * bitmaps.length;
    }
}
