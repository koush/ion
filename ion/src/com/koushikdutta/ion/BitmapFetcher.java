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
                final BitmapCallback callback = new LoadBitmapBase(ion, downloadKey, put);
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

    private static final int MAX_IMAGEVIEW_LOAD = 20;

    static boolean shouldDeferImageView(Ion ion) {
        if (ion.bitmapsPending.keySet().size() <= MAX_IMAGEVIEW_LOAD)
            return false;
        int loadCount = 0;
        for (String key: ion.bitmapsPending.keySet()) {
            Object owner = ion.bitmapsPending.tag(key);
            if (owner instanceof LoadBitmapBase) {
                loadCount++;
                if (loadCount > MAX_IMAGEVIEW_LOAD)
                    return true;
            }
        }
        return false;
    }

    static void processDeferred(Ion ion) {
        if (shouldDeferImageView(ion))
            return;
        ArrayList<DeferredLoadBitmap> deferred = null;
        for (String key: ion.bitmapsPending.keySet()) {
            Object owner = ion.bitmapsPending.tag(key);
            if (owner instanceof DeferredLoadBitmap) {
                DeferredLoadBitmap deferredLoadBitmap = (DeferredLoadBitmap)owner;
                if (deferred == null)
                    deferred = new ArrayList<DeferredLoadBitmap>();
                deferred.add(deferredLoadBitmap);
            }
        }

        if (deferred == null)
            return;
        int count = 0;
        for (DeferredLoadBitmap deferredLoadBitmap: deferred) {
            ion.bitmapsPending.tag(deferredLoadBitmap.key, null);
            ion.bitmapsPending.tag(deferredLoadBitmap.fetcher.bitmapKey, null);
            deferredLoadBitmap.fetcher.executeNetwork();
            count++;
            // do MAX_IMAGEVIEW_LOAD max. this may end up going over the MAX_IMAGEVIEW_LOAD threshhold
            if (count > MAX_IMAGEVIEW_LOAD)
                return;
        }
    }

    DeferredLoadBitmap executeDeferred() {
        DeferredLoadBitmap ret = new DeferredLoadBitmap(builder.ion, downloadKey, this);
        executeTransforms(builder.ion);
        return ret;
    }

    private void executeTransforms(Ion ion) {
        // if there's a transform, do it
        if (!hasTransforms)
            return;

        // verify this transform isn't already pending
        // make sure that the parent download isn't cancelled (empty list)
        // and also make sure there are waiters for this transformed bitmap
        if (ion.bitmapsPending.tag(bitmapKey) == null) {
            ion.bitmapsPending.add(downloadKey, new TransformBitmap(ion, bitmapKey, downloadKey, transforms));
        }
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
        if (ion.bitmapsPending.tag(downloadKey) == null && !fastLoad()) {
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

        executeTransforms(ion);
    }
}
