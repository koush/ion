package com.koushikdutta.ion;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.libcore.DiskLruCache;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.Transform;

import java.util.ArrayList;

class BitmapFetcher {
    String downloadKey;
    String bitmapKey;
    BitmapInfo info;
    boolean hasTransforms;
    ArrayList<Transform> transforms;
    IonRequestBuilder builder;
    int resizeWidth;
    int resizeHeight;
    boolean animateGif;

    boolean fastLoad() {
        Ion ion = builder.ion;
        boolean put = !hasTransforms;

        for (Loader loader: ion.configure().getLoaders()) {

            Future<BitmapInfo> future = loader.loadBitmap(ion, builder.uri, resizeWidth, resizeHeight);
            if (future != null) {
                final BitmapCallback callback = new BitmapCallback(ion, downloadKey, put);
                future.setCallback(new FutureCallback<BitmapInfo>() {
                    @Override
                    public void onCompleted(Exception e, BitmapInfo result) {
                        if (result != null)
                            result.key = downloadKey;
                        callback.report(e, result);
                    }
                });
                return true;
            }
        }
        return false;
    }

    void executeNetwork() {
        final Ion ion = builder.ion;

        // bitmaps that were transformed are put into the DiskLruCache to prevent
        // subsequent retransformation. See if we can retrieve the bitmap from the disk cache.
        // See TransformBitmap for where the cache is populated.
        DiskLruCache diskLruCache = ion.responseCache.getDiskLruCache();
        if (!builder.noCache && hasTransforms && diskLruCache.containsKey(bitmapKey)) {
            TransformBitmap.getBitmapSnapshot(ion, bitmapKey);
            return;
        }

        // Perform a download as necessary.
        if (!ion.bitmapsPending.contains(downloadKey) && !fastLoad()) {
            builder.setHandler(null);
            // if we cancel, gotta remove any waiters.
            IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform = builder.execute(new ByteBufferListParser(), new Runnable() {
                @Override
                public void run() {
                    AsyncServer.post(Ion.mainHandler, new Runnable() {
                        @Override
                        public void run() {
                            ion.bitmapsPending.remove(downloadKey);
                        }
                    });
                }
            });
            emitterTransform.setCallback(new LoadBitmap(ion, downloadKey, !hasTransforms, resizeWidth, resizeHeight, animateGif, emitterTransform));
        }

        // if there's a transform, do it
        if (!hasTransforms)
            return;

        // verify this transform isn't already pending
        // make sure that the parent download isn't cancelled (empty list)
        // and also make sure there are waiters for this transformed bitmap
        if (!ion.bitmapsPending.contains(downloadKey) || !ion.bitmapsPending.contains(bitmapKey)) {
            ion.bitmapsPending.add(downloadKey, new TransformBitmap(ion, bitmapKey, downloadKey, transforms));
        }
    }

}
