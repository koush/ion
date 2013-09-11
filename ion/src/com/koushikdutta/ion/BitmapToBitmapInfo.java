package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.async.http.libcore.DiskLruCache;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.Transform;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

class BitmapToBitmapInfo extends BitmapCallback implements FutureCallback<BitmapInfo> {
    ArrayList<Transform> transforms;

    public static void getBitmapSnapshot(final Ion ion, final String transformKey) {
        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final LoadBitmapStream callback = new LoadBitmapStream(ion, transformKey, true, 0, 0);
                try {
                    DiskLruCache.Snapshot snapshot = ion.getResponseCache().getDiskLruCache().get(transformKey);
                    try {
                        callback.loadInputStream(snapshot.getInputStream(0));
                    }
                    finally {
                        snapshot.close();
                    }
                }
                catch (Exception e) {
                    callback.report(e, null);
                    try {
                        ion.getResponseCache().getDiskLruCache().remove(transformKey);
                    }
                    catch (Exception ex) {
                    }
                }
            }
        });
    }

    public BitmapToBitmapInfo(Ion ion, String transformKey, ArrayList<Transform> transforms) {
        super(ion, transformKey, true);
        this.transforms = transforms;
    }

    @Override
    public void onCompleted(Exception e, final BitmapInfo result) {
        if (e != null) {
            report(e, null);
            return;
        }

        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                BitmapInfo info = new BitmapInfo();
                try {
                    Bitmap tmpBitmap = result.bitmap;
                    for (Transform transform : transforms) {
//                            builder.request.logd("applying transform: " + transform.key());
                        tmpBitmap = transform.transform(tmpBitmap);
                    }
                    info.loadedFrom = result.loadedFrom;
                    info.bitmap = tmpBitmap;
                    info.key = key;
                    report(null, info);
                } catch (Exception e) {
                    report(e, null);
                    return;
                }
                // the transformed bitmap was successfully load it, let's toss it into
                // the disk lru cache.
                try {
                    DiskLruCache cache = ion.getResponseCache().getDiskLruCache();
                    if (cache == null)
                        return;
                    DiskLruCache.Editor editor = cache.edit(key);
                    if (editor == null)
                        return;
                    try {
                        for (int i = 1; i < ResponseCacheMiddleware.ENTRY_COUNT; i++) {
                            editor.set(0, key);
                        }
                        OutputStream out = editor.newOutputStream(0);
                        Bitmap.CompressFormat format = info.bitmap.hasAlpha() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                        info.bitmap.compress(format, 100, out);
                        out.close();
                        editor.commit();
                    }
                    catch (Exception ex) {
                        editor.abort();
                    }
                }
                catch (Exception e) {
                }
            }
        });
    }
}