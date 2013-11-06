package com.koushikdutta.ion.bitmap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.koushikdutta.ion.Ion;

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
        BitmapInfo ret = cache.get(key);
        if (ret == null || ret.bitmaps != null)
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

    public Bitmap loadBitmap(byte[] bytes, int offset, int length, int minx, int miny) {
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
        if (targetWidth != Integer.MAX_VALUE || targetHeight != Integer.MAX_VALUE) {
            o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, offset, length, o);
            if (o.outWidth < 0 || o.outHeight < 0)
                return null;
            int scale = Math.max(o.outWidth / targetWidth, o.outHeight / targetHeight);
            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, offset, length, o);

        int rotation = Exif.getOrientation(bytes, offset, length);
        if (rotation == 0)
            return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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
        if (targetWidth != Integer.MAX_VALUE || targetHeight != Integer.MAX_VALUE) {
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
