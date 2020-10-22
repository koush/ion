package com.koushikdutta.ion.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;

import com.koushikdutta.ion.ResponseServedFrom;
import com.koushikdutta.ion.gif.GifDecoder;
import com.koushikdutta.ion.util.UntypedHashtable;

import java.io.File;

/**
 * Created by koush on 6/12/13.
 */
public class BitmapInfo {
    public BitmapInfo(String key, String mimeType, Bitmap bitmap, Point originalSize) {
        this.originalSize = originalSize;
        this.bitmap = bitmap;
        this.key = key;
        this.mimeType = mimeType;
    }

    final public Point originalSize;
    public long loadTime = System.currentTimeMillis();
    public long drawTime;
    final public String key;
    public ResponseServedFrom servedFrom;
    final public Bitmap bitmap;
    public Throwable exception;
    public GifDecoder gifDecoder;
    public BitmapRegionDecoder decoder;
    public File decoderFile;
    public final String mimeType;
    public final UntypedHashtable extras = new UntypedHashtable();

    public int sizeOf() {
        if (bitmap != null)
            return bitmap.getRowBytes() * bitmap.getHeight();
        if (gifDecoder != null)
            return gifDecoder.getGifDataLength();
        return 0;
    }
}
