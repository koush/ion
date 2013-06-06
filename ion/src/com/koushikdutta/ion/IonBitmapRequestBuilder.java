package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.ion.builder.IonImageViewRequestBuilder;
import com.koushikdutta.ion.builder.IonMutableBitmapRequestBuilder;
import com.koushikdutta.ion.builder.IonMutableBitmapRequestPostLoadBuilder;
import com.koushikdutta.ion.builder.IonImageViewRequestPostLoadBuilder;
import com.koushikdutta.ion.builder.IonImageViewRequestPreLoadBuilder;
import com.koushikdutta.ion.bitmap.Transform;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by koush on 5/23/13.
 */
class IonBitmapRequestBuilder implements IonMutableBitmapRequestBuilder, IonMutableBitmapRequestPostLoadBuilder, IonImageViewRequestPreLoadBuilder, IonImageViewRequestPostLoadBuilder {
    IonRequestBuilder builder;
    Ion ion;

    @Override
    public Future<Bitmap> load(String uri) {
        builder.load(uri);
        return intoImageView(imageViewPostRef.get());
    }

    @Override
    public Future<Bitmap> load(String method, String url) {
        builder.load(method, url);
        return intoImageView(imageViewPostRef.get());
    }

    WeakReference<ImageView> imageViewPostRef;
    IonBitmapRequestBuilder withImageView(ImageView imageView) {
        imageViewPostRef = new WeakReference<ImageView>(imageView);
        return this;
    }

    public IonBitmapRequestBuilder(IonRequestBuilder builder) {
        this.builder = builder;
        ion = builder.ion;
    }

    ArrayList<Transform> transforms;

    @Override
    public IonBitmapRequestBuilder transform(Transform transform) {
        if (transforms == null)
            transforms = new ArrayList<Transform>();
        transforms.add(transform);
        return this;
    }

    class ByteArrayToBitmapFuture extends MutateFuture<Bitmap, ByteBufferList> {
        Handler handler;
        Ion ion;
        String urlKey;

        public ByteArrayToBitmapFuture(Ion ion, Handler handler, String urlKey) {
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
        public void onCompleted(Exception e, ByteBufferList result) {
            if (e != null) {
                AsyncServer.post(handler, new Setter(e, null));
                return;
            }

            builder.request.logd("Image file size: " + result.remaining());
            final ByteArrayInputStream bin = new ByteArrayInputStream(result.getAllByteArray());
            result.clear();

            ion.executorService.execute(new Runnable() {
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

    class BitmapToBitmap extends MutateFuture<IonBitmapCache.ZombieDrawable, Bitmap> {
        Handler handler;
        Ion ion;
        String transformKey;
        ArrayList<Transform> transforms;

        public BitmapToBitmap(Ion ion, Handler handler, String transformKey, ArrayList<Transform> transforms) {
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
            ion.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap tmpBitmap = result;
                        for (Transform transform : transforms) {
                            builder.request.logd("applying transform: " + transform.getKey());
                            tmpBitmap = transform.transform(tmpBitmap);
                        }
                        AsyncServer.post(handler, new Setter(null, tmpBitmap));
                    }
                    catch (Exception e) {
                        AsyncServer.post(handler, new Setter(e, null));
                    }
                }
            });
        }

        @Override
        public void onCompleted(Exception e, Bitmap result) {
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

        if (imageView != null)
            ion.pendingViews.remove(imageView);

        // determine the key for this bitmap after all transformations
        String tmpKey = builder.request.getUri().toString();
        if (transforms != null) {
            for (Transform transform : transforms) {
                tmpKey += transform.getKey();
            }
        }
        final String transformKey = tmpKey;

        // the future to return to the caller
        final SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();

        // see if this request can be fulfilled from the cache
        BitmapDrawable bd = builder.ion.bitmapCache.getDrawable(transformKey);
        if (bd != null) {
            setImageView(imageView, bd);
            doAnimation(imageView, null);
            ret.setComplete(bd.getBitmap());
            return ret;
        }

        // need to either load/transform this, so set up the loading images, and mark this view
        // as in progress.
        if (imageView != null)
            ion.pendingViews.put(imageView, transformKey);
        setPlaceholder(imageView);

        // find/create the future for this download.
        String urlKey = builder.request.getUri().toString();
        ByteArrayToBitmapFuture pendingDownload = ion.pendingDownloads.get(urlKey);
        if (pendingDownload == null || pendingDownload.isCancelled()) {
            pendingDownload = new ByteArrayToBitmapFuture(ion, builder.handler, urlKey);
            ion.pendingDownloads.put(urlKey, pendingDownload);
            // allow the bitmap load to cancel the downloader
            pendingDownload.setMutateFuture(builder.execute(new ByteBufferListParser()));
        }

        // find/create the future for the bitmap transform
        BitmapToBitmap pendingTransform = ion.pendingTransforms.get(transformKey);
        if (pendingTransform == null || pendingTransform.isCancelled()) {
            pendingTransform = new BitmapToBitmap(ion, builder.handler, transformKey, transforms);
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
                    // see if the ImageView is still waiting for the same transform key as before
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

    @Override
    public Future<Bitmap> asBitmap() {
        return intoImageView(null);
    }

    @Override
    public IonBitmapRequestBuilder placeholder(Bitmap bitmap) {
        placeholder(new BitmapDrawable(builder.context.get().getResources(), bitmap));
        return this;
    }

    Drawable placeholderDrawable;

    @Override
    public IonBitmapRequestBuilder placeholder(Drawable drawable) {
        placeholderDrawable = drawable;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder placeholder(int resourceId) {
        if (resourceId == 0) {
            placeholderDrawable = null;
            return this;
        }
        placeholder(builder.context.get().getResources().getDrawable(resourceId));
        return this;
    }

    @Override
    public IonBitmapRequestBuilder error(Bitmap bitmap) {
        error(new BitmapDrawable(builder.context.get().getResources(), bitmap));
        return this;
    }

    Drawable errorDrawable;

    @Override
    public IonBitmapRequestBuilder error(Drawable drawable) {
        errorDrawable = drawable;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder error(int resourceId) {
        if (resourceId == 0) {
            errorDrawable = null;
            return this;
        }
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
        setImageView(imageView, placeholderDrawable);
        doAnimation(imageView, loadAnimation);
    }

    private void setErrorImage(ImageView imageView) {
        if (imageView == null)
            return;
        boolean usingPlaceholder = false;
        Drawable drawable = errorDrawable;
        if (drawable == null) {
            drawable = placeholderDrawable;
            usingPlaceholder = true;
        }
        setImageView(imageView, drawable);
        if (!usingPlaceholder)
            doAnimation(imageView, inAnimation);
    }

    Animation inAnimation;

    @Override
    public IonBitmapRequestBuilder animateIn(Animation in) {
        inAnimation = in;
        return this;
    }

    Animation loadAnimation;

    @Override
    public IonBitmapRequestBuilder animateLoad(Animation load) {
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

        imageView.startAnimation(animation);
    }

    @Override
    public IonImageViewRequestBuilder animateLoad(int animationResource) {
        return animateLoad(AnimationUtils.loadAnimation(builder.context.get(), animationResource));
    }

    @Override
    public IonImageViewRequestPostLoadBuilder animateIn(int animationResource) {
        return animateIn(AnimationUtils.loadAnimation(builder.context.get(), animationResource));
    }
}
