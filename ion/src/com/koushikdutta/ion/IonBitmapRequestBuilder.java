package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.ion.bitmap.Transform;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by koush on 5/23/13.
 */
public class IonBitmapRequestBuilder implements IonRequestBuilderStages.IonMutableBitmapRequestBuilder {
    IonRequestBuilder builder;
    Ion ion;
    public IonBitmapRequestBuilder(IonRequestBuilder builder) {
        this.builder = builder;
        ion = builder.ion;
    }

    ArrayList<Transform> transforms;
    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder transform(Transform transform) {
        if (transforms == null)
            transforms = new ArrayList<Transform>();
        transforms.add(transform);
        return this;
    }

    @Override
    public Future<Bitmap> intoImageView(ImageView imageView) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();

        final String url = builder.request.getUri().toString();
        ion.pendingViews.put(imageView, url);

        final SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();
        BitmapDrawable bd = builder.ion.bitmapCache.getDrawable(url);
        if (bd != null) {
            if (imageView != null)
                imageView.setImageDrawable(bd);
            ret.setComplete(bd.getBitmap());
            return ret;
        }

        ArrayList<SimpleFuture<BitmapDrawable>> pd = ion.pendingDownloads.get(url);
        boolean needsLoad = pd == null;
        if (pd == null) {
            pd = new ArrayList<SimpleFuture<BitmapDrawable>>();
            ion.pendingDownloads.put(url, pd);
        }
        final ArrayList<SimpleFuture<BitmapDrawable>> pendingDownloads = pd;

        // to post it all back onto the ui thread.
        final WeakReference<ImageView> iv = new WeakReference<ImageView>(imageView);
        SimpleFuture<BitmapDrawable> waiter = new SimpleFuture<BitmapDrawable>();
        waiter.setCallback(new FutureCallback<BitmapDrawable>() {
            @Override
            public void onCompleted(final Exception e, final BitmapDrawable result) {
                assert Thread.currentThread() == Looper.getMainLooper().getThread();

                // see if the imageview is still alive
                ImageView imageView = iv.get();
                if (imageView == null)
                    return;

                // see if it's still waiting for the same url as before
                String waitingUrl = ion.pendingViews.get(imageView);
                if (!TextUtils.equals(waitingUrl, url))
                    return;

                ion.pendingViews.remove(imageView);
                if (e != null) {
                    imageView.setImageBitmap(null);
                    ret.setComplete(e);
                    return;
                }

                imageView.setImageDrawable(result);
                ret.setComplete(result.getBitmap());
            }
        });

        pendingDownloads.add(waiter);

        if (!needsLoad)
            return ret;

        builder.setHandler(null);
        final Handler handler = new Handler(Looper.getMainLooper());
        builder.execute(new ByteArrayBody()).setCallback(new FutureCallback<byte[]>() {
            private void error(final Exception e) {
                AsyncServer.post(handler, new Runnable() {
                    @Override
                    public void run() {
                        while (pendingDownloads.size() > 0) {
                            pendingDownloads.remove(0).setComplete(e);
                        }
                    }
                });
            }
            @Override
            public void onCompleted(Exception e, byte[] result) {
                if (e != null) {
                    error(e);
                    return;
                }

                final ByteArrayInputStream bin = new ByteArrayInputStream(result);

                // load the bitmap on a separate thread
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Bitmap bmp = ion.bitmapCache.loadBitmapFromStream(bin);
                            if (bmp == null)
                                throw new Exception("bitmap failed to load");

                            // set the bitmap back on the handler thread
                            AsyncServer.post(handler, new Runnable() {
                                @Override
                                public void run() {
                                    IonBitmapCache.ZombieDrawable zd = ion.bitmapCache.put(url, bmp);
                                    while (pendingDownloads.size() > 0) {
                                        BitmapDrawable bd = zd.cloneAndIncrementRefCounter();
                                        pendingDownloads.remove(0).setComplete(bd);
                                    }
                                }
                            });
                        }
                        catch (Exception e) {
                            error(e);
                        }
                    }
                });

            }
        });

        return ret;
    }

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Override
    public Future<Bitmap> asBitmap() {
        return intoImageView(null);
    }
}
