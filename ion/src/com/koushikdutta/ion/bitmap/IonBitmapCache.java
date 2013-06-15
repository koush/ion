package com.koushikdutta.ion.bitmap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    Resources mResources;
    DisplayMetrics mMetrics;
    LruBitmapCache mCache;
    Ion ion;

    public IonBitmapCache(Ion ion) {
        Context context = ion.getContext();
        this.ion = ion;
        mMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(mMetrics);
        final AssetManager mgr = context.getAssets();
        mResources = new Resources(mgr, mMetrics, context.getResources().getConfiguration());
        mCache = new LruBitmapCache(getHeapSize(context) / 7);
    }

    public void put(BitmapInfo info) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        mCache.put(info.key, info);
    }

    public BitmapInfo get(String key) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        return mCache.get(key);
    }

    public void dump() {
        Log.i("IonBitmapCache", "bitmap cache: " + mCache.size());
        Log.i("IonBitmapCache", "freeMemory: " + Runtime.getRuntime().freeMemory());
    }

    public Bitmap loadBitmap(byte[] bytes, int minx, int miny) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();
        int targetWidth = minx;
        int targetHeight = miny;
        if (targetWidth <= 0)
            targetWidth = mMetrics.widthPixels;
        if (targetWidth <= 0)
            targetWidth = Integer.MAX_VALUE;
        if (targetHeight <= 0)
            targetHeight = mMetrics.heightPixels;
        if (targetHeight <= 0)
            targetHeight = Integer.MAX_VALUE;

        BitmapFactory.Options o = null;
        if (targetWidth != Integer.MAX_VALUE || targetHeight != Integer.MAX_VALUE) {
            o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
            int scale = Math.min(o.outWidth / targetWidth, o.outHeight / targetHeight);
            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
    }

    private static int getHeapSize(final Context context) {
        return ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;
    }
}
