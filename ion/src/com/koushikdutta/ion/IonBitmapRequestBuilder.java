package com.koushikdutta.ion;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.ImageView;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
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

    static class ByteArrayToBitmapFuture extends MutateFuture<Bitmap, byte[]> {
        ExecutorService executorService;
        Handler handler;
        Ion ion;
        String urlKey;
        public ByteArrayToBitmapFuture(Ion ion, Handler handler, String urlKey, ExecutorService executorService) {
            this.executorService = executorService;
            this.handler = handler;
            this.urlKey = urlKey;
            this.ion = ion;
        }
        class Setter implements Runnable {
            Bitmap bmp;
            Exception e;
            Setter(Exception e, Bitmap bmp) {
                this.bmp = bmp;
                this.e = e;
            }
            @Override
            public void run() {
                ion.pendingDownloads.remove(urlKey);
                if (e != null)
                    setComplete(e);
                else
                    setComplete(bmp);
                bmp = null;
                e = null;
            }
        }
        @Override
        public void onCompleted(Exception e, byte[] result) {
            assert Thread.currentThread() == Looper.getMainLooper().getThread();

            if (e != null) {
                AsyncServer.post(handler, new Setter(e, null));
                return;
            }

            final ByteArrayInputStream bin = new ByteArrayInputStream(result);

            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap bmp = ion.bitmapCache.loadBitmapFromStream(bin);
                        if (bmp == null)
                            throw new Exception("bitmap failed to load");
                        AsyncServer.post(handler, new Setter(null, bmp));
                    }
                    catch (Exception e) {
                        AsyncServer.post(handler, new Setter(e, null));
                    }
                }
            });
        }
    }

    static class BitmapToBitmap extends MutateFuture<IonBitmapCache.ZombieDrawable, Bitmap> {
        ExecutorService executorService;
        Handler handler;
        Ion ion;
        String transformKey;
        ArrayList<Transform> transforms;
        public BitmapToBitmap(Ion ion, Handler handler, String transformKey, ArrayList<Transform> transforms, ExecutorService executorService) {
            this.executorService = executorService;
            this.handler = handler;
            this.transformKey = transformKey;
            this.transforms = transforms;
            this.ion = ion;
        }
        class Setter implements Runnable {
            Bitmap bmp;
            Exception e;
            Setter(Exception e, Bitmap bmp) {
                this.bmp = bmp;
                this.e = e;
            }
            @Override
            public void run() {
                ion.pendingTransforms.remove(transformKey);
                if (e != null) {
                    setComplete(e);
                }
                else {
                    IonBitmapCache.ZombieDrawable bd = ion.bitmapCache.put(transformKey, bmp);
                    setComplete(bd);
                }
                bmp = null;
                e = null;
            }
        }

        public void transform(final Bitmap result) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap tmpBitmap = result;
                        for (Transform transform : transforms) {
                            tmpBitmap = transform.transform(tmpBitmap);
                        }
                        AsyncServer.post(handler, new Setter(null, tmpBitmap));
                    } catch (Exception e) {
                        AsyncServer.post(handler, new Setter(e, null));
                    }
                }
            });
        }

        @Override
        public void onCompleted(Exception e, Bitmap result) {
            assert Thread.currentThread() == Looper.getMainLooper().getThread();

            if (e != null) {
                AsyncServer.post(handler, new Setter(e, null));
                return;
            }

            // no transform necessary
            if (transforms == null || transforms.size() == 0) {
                AsyncServer.post(handler, new Setter(null, result));
                return;
            }

            transform(result);
        }
    }

    @Override
    public Future<Bitmap> intoImageView(ImageView imageView) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();

//        ResponseCacheMiddleware cache = ion.responseCache;
//        IonLog.i("cache hits: " + cache.getCacheHitCount());
//        IonLog.i("cond cache hits: " + cache.getConditionalCacheHitCount());

        // determine the key for this bitmap after all transformations
        String tmpKey = builder.request.getUri().toString();
        if (transforms != null) {
            for (Transform transform: transforms) {
                tmpKey += transform.getKey();
            }
        }
        final String transformKey = tmpKey;

        // the future to return to the caller
        final SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();

        // see if this request can be fulfilled from the cache
        BitmapDrawable bd = builder.ion.bitmapCache.getDrawable(transformKey);
        if (bd != null) {
            if (imageView != null)
                imageView.setImageDrawable(bd);
            ret.setComplete(bd.getBitmap());
            return ret;
        }

        // need to either load/transform this, so set up the loading images, and mark this view
        // as in progress.
        ion.pendingViews.put(imageView, transformKey);
        setPlaceholder(imageView);

        final Handler handler = new Handler();

        // find/create the future for this download.
        String urlKey = builder.request.getUri().toString();
        ByteArrayToBitmapFuture pendingDownload = ion.pendingDownloads.get(urlKey);
        if (pendingDownload == null) {
            pendingDownload = new ByteArrayToBitmapFuture(ion, handler, urlKey, executorService);
            ion.pendingDownloads.put(transformKey, pendingDownload);
            // allow the bitmap load to cancel the downlaoder
            pendingDownload.setMutateFuture(builder.execute(new ByteArrayBody()));
        }

        // find/create the future for the bitmap transform
        BitmapToBitmap pendingTransform = ion.pendingTransforms.get(transformKey);
        if (pendingTransform == null) {
            pendingTransform = new BitmapToBitmap(ion, handler, transformKey, transforms, executorService);
            ion.pendingTransforms.put(transformKey, pendingTransform);
            // allow the bitmap transform to cancel the bitmap load
            pendingDownload.addChildFuture(pendingTransform.createMutateFuture());
        }

        // note whether an ImageView was present during invocation, as
        // only a weak reference is held from here on out.
        final WeakReference<ImageView> iv = new WeakReference<ImageView>(imageView);

        // get a child future that can be used to set the ImageView once the drawable is ready
        SimpleFuture<IonBitmapCache.ZombieDrawable> drawableFuture = new SimpleFuture<IonBitmapCache.ZombieDrawable>();
        drawableFuture.setCallback(new FutureCallback<IonBitmapCache.ZombieDrawable>() {
            @Override
            public void onCompleted(final Exception e, IonBitmapCache.ZombieDrawable source) {
                assert Thread.currentThread() == Looper.getMainLooper().getThread();

                // see if the imageview is still alive and cares about this result
                ImageView imageView = iv.get();
                if (imageView != null) {
                    // see if it's still waiting for the same url key as before
                    String waitingKey = ion.pendingViews.get(imageView);
                    if (!TextUtils.equals(waitingKey, transformKey))
                        imageView = null;
                    else
                        ion.pendingViews.remove(imageView);
                }

                if (e != null) {
                    setErrorImage(imageView);
                    ret.setComplete(e);
                    return;
                }

                IonBitmapCache.ZombieDrawable result = source.cloneAndIncrementRefCounter();
                if (imageView != null) {
                    imageView.setImageDrawable(result);
                    doAnimation(imageView, inAnimation);
                }
                ret.setComplete(result.getBitmap());
            }
        });

        pendingTransform.addChildFuture(drawableFuture);

        // the user returned future can cancel the future that sets the drawable
        ret.setParent(drawableFuture);

        return ret;
    }

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Override
    public Future<Bitmap> asBitmap() {
        return intoImageView(null);
    }

    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder placeholder(Bitmap bitmap) {
        placeholder(new BitmapDrawable(builder.context.get().getResources(), bitmap));
        return this;
    }

    Drawable placeholderDrawable;
    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder placeholder(Drawable drawable) {
        placeholderDrawable = drawable;
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder placeholder(int resourceId) {
        placeholder(builder.context.get().getResources().getDrawable(resourceId));
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder error(Bitmap bitmap) {
        error(new BitmapDrawable(builder.context.get().getResources(), bitmap));
        return this;
    }

    Drawable errorDrawable;
    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder error(Drawable drawable) {
        errorDrawable = drawable;
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder error(int resourceId) {
        error(builder.context.get().getResources().getDrawable(resourceId));
        return this;
    }

    private static void setImageView(ImageView imageView, Drawable drawable) {
        if (imageView == null)
            return;
        imageView.setImageDrawable(drawable);
    }

    private void setPlaceholder(ImageView imageView) {
        if (imageView == null)
            return;
        doAnimation(imageView, loadAnimation);
        setImageView(imageView, placeholderDrawable);
    }

    private void setErrorImage(ImageView imageView) {
        if (imageView == null)
            return;
        doAnimation(imageView, inAnimation);
        setImageView(imageView, errorDrawable);
    }

    Animation inAnimation;
    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder animateIn(Animation in) {
        inAnimation = in;
        return this;
    }

    Animation loadAnimation;
    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder animateLoad(Animation load) {
        loadAnimation = load;
        return this;
    }

    private void doAnimation(ImageView imageView, Animation animation) {
        if (imageView == null)
            return;
        if (animation == null) {
            imageView.setAnimation(null);
            return;
        }

        imageView.setAnimation(animation);
        animation.start();
    }
}
