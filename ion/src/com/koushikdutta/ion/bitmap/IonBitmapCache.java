package com.koushikdutta.ion.bitmap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
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
        return cache.removeBitmapInfo(key);
    }

    public void clear() {
        cache.evictAllBitmapInfo();
    }

    public void put(BitmapInfo info) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        if (getHeapSize(ion.getContext()) != cache.maxSize())
            cache.setMaxSize(getHeapSize(ion.getContext()) / 7);
        cache.put(info.key, info);
    }

    public BitmapInfo get(String key) {
        if (key == null)
            return null;

        // see if this thing has an immediate cache hit
        BitmapInfo ret = cache.getBitmapInfo(key);
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

    private Point computeTarget(int minx, int miny) {
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
        return new Point(targetWidth, targetHeight);
    }

    public Bitmap loadBitmap(byte[] bytes, int offset, int length, int minx, int miny) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();
        Point target = computeTarget(minx, miny);

        BitmapFactory.Options o = null;
        if (target.x != Integer.MAX_VALUE || target.y != Integer.MAX_VALUE) {
            o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, offset, length, o);
            if (o.outWidth < 0 || o.outHeight < 0)
                return null;
            int scale = Math.max(o.outWidth / target.x, o.outHeight / target.y);
            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
        }

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeByteArray(bytes, offset, length, o);
        }
        catch (OutOfMemoryError e) {
            return null;
        }
        if (bitmap == null)
            return null;

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
        Point target = computeTarget(minx, miny);

        int rotation;
        try {
            byte[] bytes = new byte[50000];
            stream.mark(Integer.MAX_VALUE);
            int length = stream.read(bytes);
            rotation = Exif.getOrientation(bytes, 0, length);
            stream.reset();
        }
        catch (Exception e) {
            rotation = 0;
        }

        BitmapFactory.Options o = null;
        if (target.x != Integer.MAX_VALUE || target.y != Integer.MAX_VALUE) {
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
            int scale = Math.max(o.outWidth / target.x, o.outHeight / target.y);
            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
        }

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(stream, null, o);
        }
        catch (OutOfMemoryError e) {
            return null;
        }
        if (bitmap == null)
            return null;

        if (rotation == 0)
            return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static int getHeapSize(final Context context) {
        return ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;
    }
}
