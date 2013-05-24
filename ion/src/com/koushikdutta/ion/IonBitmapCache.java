package com.koushikdutta.ion;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import com.koushikdutta.ion.bitmap.DrawableCache;
import com.koushikdutta.ion.bitmap.LruBitmapCache;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

/**
 * Created by koush on 5/23/13.
 */
class IonBitmapCache {
    private static DrawableCache<ZombieDrawable> mLiveCache = new DrawableCache<ZombieDrawable>();
    private static LruBitmapCache mDeadCache;
    // this cache is simply to maintain a reference to the bitmap
    // so that when the zombie drawable is garbage collected, the bitmap doesn't
    // automatically get collected with it.
    private static HashSet<Bitmap> mAllCache = new HashSet<Bitmap>();
    static Resources mResources;
    static DisplayMetrics mMetrics;

    Ion ion;
    public IonBitmapCache(Ion ion) {
        this.ion = ion;
        mDeadCache = new LruBitmapCache(getHeapSize(ion.getContext()));
        mMetrics = new DisplayMetrics();
        ((WindowManager) ion.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(mMetrics);
        final AssetManager mgr = ion.getContext().getAssets();
        mResources = new Resources(mgr, mMetrics, ion.getContext().getResources().getConfiguration());
    }

    ZombieDrawable put(String url, Bitmap bitmap) {
        if (bitmap == null)
            return null;
        return new ZombieDrawable(url, bitmap);
    }

    boolean useBitmapScaling = true;
    Bitmap loadBitmapFromStream(InputStream in) throws IOException {
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
            return BitmapFactory.decodeStream(in, null, o);
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

    public BitmapDrawable getDrawable(String url) {
        if (TextUtils.isEmpty(url))
            return null;

        ZombieDrawable live = mLiveCache.get(url);
        if (live != null)
            return live.cloneAndIncrementRefCounter();

        Bitmap bitmap = mDeadCache.remove(url);
        if (bitmap == null)
            return null;

        return new ZombieDrawable(url, bitmap);
    }


    private static int getHeapSize(final Context context) {
        return ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;
    }

    private static class Brains {
        int mRefCounter;
        boolean mHeadshot;
        long loadTime = System.currentTimeMillis();
    }
    /***
     * ZombieDrawable refcounts Bitmaps by hooking the finalizer.
     *
     */
    class ZombieDrawable extends BitmapDrawable {
        public ZombieDrawable(final String url, final Bitmap bitmap) {
            this(url, bitmap, new Brains());
        }

        Brains mBrains;
        private ZombieDrawable(final String url, final Bitmap bitmap, Brains brains) {
            super(mResources, bitmap);
            mUrl = url;
            mBrains = brains;

            mAllCache.add(bitmap);
            mDeadCache.remove(url);
            mLiveCache.put(url, this);

            mBrains.mRefCounter++;
        }

        public ZombieDrawable cloneAndIncrementRefCounter() {
            return new ZombieDrawable(mUrl, getBitmap(), mBrains);
        }

        String mUrl;

        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            mBrains.mRefCounter--;
            if (mBrains.mRefCounter == 0) {
                if (!mBrains.mHeadshot)
                    mDeadCache.put(mUrl, getBitmap());
                mAllCache.remove(getBitmap());
                mLiveCache.remove(mUrl);
            }
        }

        // kill this zombie, forever.
        public void headshot() {
            mBrains.mHeadshot = true;
            mLiveCache.remove(mUrl);
            mAllCache.remove(getBitmap());
        }
    }
}
