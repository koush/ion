package com.koushikdutta.ion;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.libcore.DiskLruCache;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.loader.MediaFile;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

class BitmapFetcher implements IonRequestBuilder.LoadRequestCallback {
    String downloadKey;
    String bitmapKey;
    BitmapInfo info;
    boolean hasTransforms;
    ArrayList<Transform> transforms;
    IonRequestBuilder builder;
    int resizeWidth;
    int resizeHeight;
    boolean animateGif;
    boolean deepZoom;

    private boolean fastLoad(String uri) {
        Ion ion = builder.ion;
        if (deepZoom) {
            if (uri == null || !uri.startsWith("file:/"))
                return false;
            File file = new File(URI.create(uri));
            if (!file.exists())
                return false;
            MediaFile.MediaFileType type = MediaFile.getFileType(file.getAbsolutePath());
            if (type == null || !MediaFile.isVideoFileType(type.fileType)) {
                LoadDeepZoom loadDeepZoom = new LoadDeepZoom(ion, downloadKey, animateGif, null, null);
                loadDeepZoom.onCompleted(null, file);
//                System.out.println("fastloading deepZoom");
                return true;
            }
            // fall through to allow some other loader to open this, cause this is a video file
        }

        boolean put = !hasTransforms;

        for (Loader loader: ion.configure().getLoaders()) {
            Future<BitmapInfo> future = loader.loadBitmap(builder.context.get(), ion, downloadKey, uri, resizeWidth, resizeHeight, animateGif);
            if (future != null) {
                final BitmapCallback callback = new LoadBitmapBase(ion, downloadKey, put);
                future.setCallback(new FutureCallback<BitmapInfo>() {
                    @Override
                    public void onCompleted(Exception e, BitmapInfo result) {
                        callback.report(e, result);
                    }
                });
                return true;
            }
        }
        return false;
    }

    public static final int MAX_IMAGEVIEW_LOAD = 5;

    public static boolean shouldDeferImageView(Ion ion) {
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

    public DeferredLoadBitmap defer() {
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

    @Override
    public boolean loadRequest(AsyncHttpRequest request) {
        return !fastLoad(request.getUri().toString());
    }

    public void execute() {
        final Ion ion = builder.ion;

        // bitmaps that were transformed are put into the DiskLruCache to prevent
        // subsequent retransformation. See if we can retrieve the bitmap from the disk cache.
        // See TransformBitmap for where the cache is populated.
        DiskLruCache diskLruCache = ion.responseCache.getDiskLruCache();
        if (!builder.noCache && hasTransforms && diskLruCache.containsKey(bitmapKey) && !deepZoom) {
            TransformBitmap.getBitmapSnapshot(ion, bitmapKey);
            return;
        }

        // Perform a download as necessary.
        if (ion.bitmapsPending.tag(downloadKey) == null && !fastLoad(builder.uri)) {
            builder.setHandler(null);
            builder.loadRequestCallback = this;

            if (!deepZoom) {
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
            else {
//                System.out.println("downloading file for deepZoom");
                File file = diskLruCache.getFile(downloadKey, 0);
                IonRequestBuilder.EmitterTransform<File> emitterTransform = builder.write(file);
                LoadDeepZoom loadDeepZoom = new LoadDeepZoom(ion, downloadKey, animateGif, emitterTransform, diskLruCache) {
                    @Override
                    public void onCompleted(Exception e, File file) {
                        super.onCompleted(e, file);
                    }
                };
                emitterTransform.setCallback(loadDeepZoom);
            }
        }

        executeTransforms(ion);
    }
}
