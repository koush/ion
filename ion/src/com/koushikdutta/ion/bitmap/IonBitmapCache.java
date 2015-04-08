package com.koushikdutta.ion.bitmap;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.io.FileInputStream;
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
        Context context = ion.getContext().getApplicationContext();
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

    double heapRatio = 1d / 7d;
    public double getHeapRatio() {
        return heapRatio;
    }

    public void setHeapRatio(double heapRatio) {
        this.heapRatio = heapRatio;
    }

    public void put(BitmapInfo info) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        int maxSize = (int)(getHeapSize(ion.getContext()) * heapRatio);
        if (maxSize != cache.maxSize())
            cache.setMaxSize(maxSize);
        cache.put(info.key, info);
    }

    public void putSoft(BitmapInfo info) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        cache.putSoft(info.key, info);
    }

    public BitmapInfo get(String key) {
        if (key == null)
            return null;

        // see if this thing has an immediate cache hit
        BitmapInfo ret = cache.getBitmapInfo(key);
        if (ret == null)
            return null;
        if (ret.bitmap != null && ret.bitmap.isRecycled()) {
            Log.w("ION", "Cached bitmap was recycled.");
            Log.w("ION", "This may happen if passing Ion bitmaps directly to notification builders or remote media clients.");
            Log.w("ION", "Create a deep copy before doing this.");
            cache.remove(key);
            return null;
        }
        if (ret.exception == null)
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

	private BitmapFactory.Options prepareBitmapOptions(BitmapFactory.Options o, int minx, int miny) throws BitmapDecodeException {
        if (o.outWidth < 0 || o.outHeight < 0)
            throw new BitmapDecodeException(o.outWidth, o.outHeight);
        Point target = computeTarget(minx, miny);
        int scale = Math.round(Math.max((float)o.outWidth / target.x, (float)o.outHeight / target.y));
        BitmapFactory.Options ret = new BitmapFactory.Options();
        ret.inSampleSize = scale;
        ret.outWidth = o.outWidth;
        ret.outHeight = o.outHeight;
        ret.outMimeType = o.outMimeType;
        return ret;
    }

    public BitmapFactory.Options prepareBitmapOptions(File file, int minx, int miny) throws BitmapDecodeException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.toString(), o);
        return prepareBitmapOptions(o, minx, miny);
    }

    public BitmapFactory.Options prepareBitmapOptions(byte[] bytes, int offset, int length, int minx, int miny) throws BitmapDecodeException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, offset, length, o);
        return prepareBitmapOptions(o, minx, miny);
    }

    public BitmapFactory.Options prepareBitmapOptions(Resources res, int id, int minx, int miny) throws BitmapDecodeException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, id, o);
        return prepareBitmapOptions(o, minx, miny);
    }

    public BitmapFactory.Options prepareBitmapOptions(InputStream in, int minx, int miny) throws BitmapDecodeException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, o);
        return prepareBitmapOptions(o, minx, miny);
    }

    private static Bitmap getRotatedBitmap(Bitmap bitmap, int rotation) {
        if (bitmap == null)
            return null;
        if (rotation == 0)
            return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap loadBitmap(byte[] bytes, int offset, int length, BitmapFactory.Options o) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, offset, length, o);
        if (bitmap == null)
            return null;
        int rotation = Exif.getOrientation(bytes, offset, length);
        return getRotatedBitmap(bitmap, rotation);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    public static Bitmap loadRegion(final BitmapRegionDecoder decoder, Rect sourceRect, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        return decoder.decodeRegion(sourceRect, options);
    }

    public static Bitmap loadBitmap(Resources res, int id, BitmapFactory.Options o) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();

        int rotation;
        InputStream in = null;
        try {
            in = res.openRawResource(id);
            byte[] bytes = new byte[50000];
            int length = in.read(bytes);
            rotation = Exif.getOrientation(bytes, 0, length);
        }
        catch (Exception e) {
            rotation = 0;
        }
        StreamUtility.closeQuietly(in);

        Bitmap bitmap = BitmapFactory.decodeResource(res, id, o);
        return getRotatedBitmap(bitmap, rotation);
    }

    public static Bitmap loadBitmap(InputStream stream, BitmapFactory.Options o) throws IOException {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();

        int rotation;
        MarkableInputStream in = new MarkableInputStream(stream);
        in.mark(50000);
        try {
            byte[] bytes = new byte[50000];
            int length = in.read(bytes);
            rotation = Exif.getOrientation(bytes, 0, length);
        }
        catch (Exception e) {
            rotation = 0;
        }
        in.reset();

        Bitmap bitmap = BitmapFactory.decodeStream(in, null, o);
        return getRotatedBitmap(bitmap, rotation);
    }

    public static Bitmap loadBitmap(File file, BitmapFactory.Options o) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();

        int rotation;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            byte[] bytes = new byte[50000];
            int length = fin.read(bytes);
            rotation = Exif.getOrientation(bytes, 0, length);
        }
        catch (Exception e) {
            rotation = 0;
        }
        StreamUtility.closeQuietly(fin);

        Bitmap bitmap = BitmapFactory.decodeFile(file.toString(), o);
        return getRotatedBitmap(bitmap, rotation);
    }

    private static int getHeapSize(final Context context) {
        return ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;
    }
}
