package com.koushikdutta.ion;

import android.graphics.Bitmap;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.io.InputStream;

class LoadBitmapStream extends BitmapCallback implements FutureCallback<InputStream> {
    int resizeWidth;
    int resizeHeight;

    public LoadBitmapStream(Ion ion, String urlKey, boolean put, int resizeWidth, int resizeHeight) {
        super(ion, urlKey, put);
        this.resizeWidth = resizeWidth;
        this.resizeHeight = resizeHeight;
    }

    public void loadInputStream(InputStream result) {
        try {
            Bitmap bitmap = ion.bitmapCache.loadBitmap(result, resizeWidth, resizeHeight);

            if (bitmap == null)
                throw new Exception("bitmap failed to load");

            BitmapInfo info = new BitmapInfo();
            info.key = key;
            info.bitmaps = new Bitmap[] { bitmap };
            info.loadedFrom = Loader.LoaderEmitter.LOADED_FROM_CACHE;

            report(null, info);
        } catch (Exception e) {
            report(e, null);
        }
    }

    @Override
    public void onCompleted(Exception e, final InputStream result) {
        if (e != null) {
            report(e, null);
            return;
        }

        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                loadInputStream(result);
            }
        });
    }
}

    