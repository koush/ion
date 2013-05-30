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
import android.view.WindowManager;

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
        Context context = ion.getContext();
        this.ion = ion;
        mDeadCache = new LruBitmapCache(getHeapSize(context));
        mMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(mMetrics);
        final AssetManager mgr = context.getAssets();
        mResources = new Resources(mgr, mMetrics, context.getResources().getConfiguration());
    }

    ZombieDrawable put(String key, Bitmap bitmap) {
        if (bitmap == null)
            return null;
        return new ZombieDrawable(key, bitmap);
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

    public BitmapDrawable getDrawable(String key) {
        if (TextUtils.isEmpty(key))
            return null;

        ZombieDrawable live = mLiveCache.get(key);
        if (live != null)
            return live.cloneAndIncrementRefCounter();

        Bitmap bitmap = mDeadCache.remove(key);
        if (bitmap == null)
            return null;

        return new ZombieDrawable(key, bitmap);
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
        public ZombieDrawable(final String key, final Bitmap bitmap) {
            this(key, bitmap, new Brains());
        }

        Brains mBrains;
        private ZombieDrawable(final String key, final Bitmap bitmap, Brains brains) {
            super(mResources, bitmap);
            this.key = key;
            mBrains = brains;

            mAllCache.add(bitmap);
            mDeadCache.remove(key);
            mLiveCache.put(key, this);

            mBrains.mRefCounter++;
        }

        public ZombieDrawable cloneAndIncrementRefCounter() {
            return new ZombieDrawable(key, getBitmap(), mBrains);
        }

        String key;

        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            mBrains.mRefCounter--;
            if (mBrains.mRefCounter == 0) {
                if (!mBrains.mHeadshot)
                    mDeadCache.put(key, getBitmap());
                mAllCache.remove(getBitmap());
                mLiveCache.remove(key);
            }
        }

        // kill this zombie, forever.
        public void headshot() {
            mBrains.mHeadshot = true;
            mLiveCache.remove(key);
            mAllCache.remove(getBitmap());
        }
    }
}
