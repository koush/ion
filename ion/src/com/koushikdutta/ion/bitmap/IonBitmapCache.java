package com.koushikdutta.ion.bitmap;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.koushikdutta.ion.Ion;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by koush on 5/23/13.
 */
public class IonBitmapCache {
    public static final long DEFAULT_ERROR_CACHE_DURATION = 30000L;

    Resources resources;
    DisplayMetrics metrics;
    LruBitmapCache cache;
    Ion ion;
    long errorCacheDuration = DEFAULT_ERROR_CACHE_DURATION;

    public long getErrorCacheDuration() {
        return errorCacheDuration;
    }

    public void setErrorCacheDuration(long errorCacheDuration) {
        this.errorCacheDuration = errorCacheDuration;
    }

    public IonBitmapCache(Ion ion) {
        Context context = ion.getContext();
        this.ion = ion;
        metrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(metrics);
        final AssetManager mgr = context.getAssets();
        resources = new Resources(mgr, metrics, context.getResources().getConfiguration());
        cache = new LruBitmapCache(getHeapSize(context) / 7);
    }

    public BitmapInfo remove(String key) {
        return cache.remove(key);
    }

    public void put(BitmapInfo info) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        cache.put(info.key, info);
    }

    public BitmapInfo get(String key) {
        if (key == null)
            return null;
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        BitmapInfo ret = cache.get(key);
        if (ret == null || ret.bitmap != null)
            return ret;

        // if this bitmap load previously errored out, see if it is time to retry
        // the fetch. connectivity error, server failure, etc, shouldn't be
        // cached indefinitely...
        if (ret.loadTime + errorCacheDuration > System.currentTimeMillis())
            return ret;
        cache.remove(key);
        return null;
    }

    public void dump() {
        Log.i("IonBitmapCache", "bitmap cache: " + cache.size());
        Log.i("IonBitmapCache", "freeMemory: " + Runtime.getRuntime().freeMemory());
    }

    private Bitmap loadRegionLegacy(byte[] bytes, int offset, int length, Rect sourceRect, BitmapFactory.Options o) {
        Bitmap source = BitmapFactory.decodeByteArray(bytes, offset, length, o);
        return Bitmap.createBitmap(source, sourceRect.left, sourceRect.top, sourceRect.width(), sourceRect.height());
    }

    @SuppressLint("NewApi")
    private Bitmap loadRegion(byte[] bytes, int offset, int length, Rect sourceRect, BitmapFactory.Options o) {
        try {
            return BitmapRegionDecoder.newInstance(bytes, offset, length, true)
            .decodeRegion(sourceRect, o);
        }
        catch (Exception e) {
            return null;
        }
    }

    public Bitmap loadBitmap(byte[] bytes, int offset, int length, RectF sourceRect, int minx, int miny) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();
        int targetWidth = minx;
        int targetHeight = miny;
        if (targetWidth == 0)
            targetWidth = metrics.widthPixels;
        if (targetWidth <= 0)
            targetWidth = Integer.MAX_VALUE;
        if (targetHeight == 0)
            targetHeight = metrics.heightPixels;
        if (targetHeight <= 0)
            targetHeight = Integer.MAX_VALUE;

        BitmapFactory.Options o = null;
        Rect rect = null;
        // see if we need the bounds before loading
        boolean needsResample = targetWidth != Integer.MAX_VALUE || targetHeight != Integer.MAX_VALUE;
        if (needsResample || sourceRect != null) {
            o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, offset, length, o);
            if (o.outWidth < 0 || o.outHeight < 0)
                return null;

            // bounds retrieved
            int outWidth = o.outWidth;
            int outHeight = o.outHeight;

            // region decoding
            if (sourceRect != null) {
                rect = new Rect();
                rect.set(
                (int)(sourceRect.left * o.outWidth),
                (int)(sourceRect.top * o.outHeight),
                (int)(sourceRect.right * o.outWidth),
                (int)(sourceRect.bottom * o.outHeight));

                // the out bounds need to be adjusted to make sense for the region
                outWidth = rect.width();
                outHeight = rect.height();
            }

            int scale = Math.max(outWidth / targetWidth, outHeight / targetHeight);
            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
        }
        if (rect == null)
            return BitmapFactory.decodeByteArray(bytes, offset, length, o);

        if (Build.VERSION.SDK_INT < 10)
            return loadRegionLegacy(bytes, offset, length, rect, o);
        return loadRegion(bytes, offset, length, rect, o);
    }

    public Bitmap loadBitmap(InputStream stream, int minx, int miny) {
        if (!stream.markSupported())
            stream = new MarkableInputStream(stream);
        assert Thread.currentThread() != Looper.getMainLooper().getThread();
        int targetWidth = minx;
        int targetHeight = miny;
        if (targetWidth <= 0)
            targetWidth = metrics.widthPixels;
        if (targetWidth <= 0)
            targetWidth = Integer.MAX_VALUE;
        if (targetHeight <= 0)
            targetHeight = metrics.heightPixels;
        if (targetHeight <= 0)
            targetHeight = Integer.MAX_VALUE;

        BitmapFactory.Options o = null;
        if (targetWidth != Integer.MAX_VALUE && targetHeight != Integer.MAX_VALUE) {
            o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            stream.mark(Integer.MAX_VALUE);
            BitmapFactory.decodeStream(stream, null, o);
            if (o.outWidth < 0 || o.outHeight < 0)
                return null;
            try {
                stream.reset();
            }
            catch (Exception e) {
                return null;
            }
            int scale = Math.max(o.outWidth / targetWidth, o.outHeight / targetHeight);
            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
        }
        return BitmapFactory.decodeStream(stream, null, o);
    }

    private static int getHeapSize(final Context context) {
        return ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;
    }
}
