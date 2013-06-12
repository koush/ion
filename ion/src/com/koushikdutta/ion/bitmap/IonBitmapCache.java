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

    boolean useBitmapScaling = true;
    public BitmapInfo loadBitmapFromStream(InputStream in, String key, int loadedFrom) throws IOException {
        assert Thread.currentThread() != Looper.getMainLooper().getThread();
        final int tw = mMetrics.widthPixels;
        final int th = mMetrics.heightPixels;
        final int targetWidth = tw <= 0 ? Integer.MAX_VALUE : tw;
        final int targetHeight = th <= 0 ? Integer.MAX_VALUE : th;

        try {
            BitmapFactory.Options o = null;
            if (useBitmapScaling) {
                o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                in.mark(in.available());
                BitmapFactory.decodeStream(in, null, o);
                in.reset();
                int scale = 0;
                while ((o.outWidth >> scale) > targetWidth || (o.outHeight >> scale) > targetHeight) {
                    scale++;
                }
                o = new BitmapFactory.Options();
                o.inSampleSize = 1 << scale;
            }
            Bitmap ret = BitmapFactory.decodeStream(in, null, o);

            BitmapInfo info = new BitmapInfo();
            info.key = key;
            info.bitmap = ret;
            info.loadedFrom = loadedFrom;
            return info;
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                }
            }
        }
    }

    private static int getHeapSize(final Context context) {
        return ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;
    }
}
