package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.async.http.libcore.DiskLruCache;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.loader.FileLoader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

class BitmapToBitmapInfo extends BitmapCallback implements FutureCallback<BitmapInfo> {
    ArrayList<Transform> transforms;

    public static void getBitmapSnapshot(final Ion ion, final String transformKey) {
        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final BitmapCallback callback = new BitmapCallback(ion, transformKey, true);
                try {
                    DiskLruCache.Snapshot snapshot = ion.responseCache.getDiskLruCache().get(transformKey);
                    try {
                        InputStream in = snapshot.getInputStream(0);
                        assert in instanceof FileInputStream;
                        Bitmap bitmap = ion.getBitmapCache().loadBitmap(new BufferedInputStream((FileInputStream)in, 1024 * 64), -1, -1);
                        in.close();
                        if (bitmap == null)
                            throw new Exception("Bitmap failed to load");

                        BitmapInfo info = new BitmapInfo();
                        info.bitmaps = new Bitmap[] { bitmap };
                        info.loadedFrom =  Loader.LoaderEmitter.LOADED_FROM_CACHE;
                        info.key = transformKey;
                        callback.report(null, info);
                    } finally {
                        snapshot.close();
                    }
                } catch (Exception e) {
                    callback.report(e, null);
                    try {
                        ion.responseCache.getDiskLruCache().remove(transformKey);
                    } catch (Exception ex) {
                    }
                }
            }
        });
    }

    String downloadKey;
    public BitmapToBitmapInfo(Ion ion, String transformKey, String downloadKey, ArrayList<Transform> transforms) {
        super(ion, transformKey, true);
        this.transforms = transforms;
        this.downloadKey = downloadKey;
    }

    @Override
    public void onCompleted(Exception e, final BitmapInfo result) {
        if (e != null) {
            report(e, null);
            return;
        }

        if (ion.bitmapsPending.tag(key) != this) {
            Log.d("IonBitmapLoader", "Bitmap transform cancelled (no longer needed)");
            return;
        }

        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                BitmapInfo info = new BitmapInfo();
                info.bitmaps = new Bitmap[result.bitmaps.length];
                try {
                    for (int i = 0; i < result.bitmaps.length; i++) {
                        for (Transform transform : transforms) {
                            info.bitmaps[i] = transform.transform(result.bitmaps[i]);
                        }
                    }
                    info.delays = result.delays;
                    info.loadedFrom = result.loadedFrom;
                    info.key = key;
                    report(null, info);
                } catch (Exception e) {
                    report(e, null);
                    return;
                }
                // the transformed bitmap was successfully load it, let's toss it into
                // the disk lru cache.
                // but don't persist gifs...
                if (info.bitmaps.length > 1)
                    return;
                try {
                    DiskLruCache cache = ion.responseCache.getDiskLruCache();
                    if (cache == null)
                        return;
                    DiskLruCache.Editor editor = cache.edit(key);
                    if (editor == null)
                        return;
                    try {
                        for (int i = 1; i < ResponseCacheMiddleware.ENTRY_COUNT; i++) {
                            editor.set(i, key);
                        }
                        OutputStream out = editor.newOutputStream(0);
                        Bitmap.CompressFormat format = info.bitmaps[0].hasAlpha() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                        info.bitmaps[0].compress(format, 100, out);
                        out.close();
                        editor.commit();
                    } catch (Exception ex) {
                        editor.abort();
                    }
                } catch (Exception e) {
                }
            }
        });
    }
}