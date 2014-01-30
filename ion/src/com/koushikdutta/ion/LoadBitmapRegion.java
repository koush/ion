package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;

import com.koushikdutta.ion.bitmap.BitmapInfo;

/**
 * Created by koush on 1/29/14.
 */
public class LoadBitmapRegion extends BitmapCallback {
    public LoadBitmapRegion(final Ion ion, final String key, final BitmapRegionDecoder decoder, final Rect region, final int inSampleSize) {
        super(ion, key, true);

        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = ion.bitmapCache.loadRegion(decoder, region, inSampleSize);
                    if (bitmap == null)
                        throw new Exception("failed to load bitmap region");
                    BitmapInfo info = new BitmapInfo(key, new Bitmap[] { bitmap }, new Point(bitmap.getWidth(), bitmap.getHeight()));
                    report(null, info);
                }
                catch (Exception e) {
                    report(e, null);
                }
            }
        });
    }
}
