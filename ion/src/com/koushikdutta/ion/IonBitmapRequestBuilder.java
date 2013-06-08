package com.koushikdutta.ion;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

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
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.builder.IonImageViewRequestBuilder;
import com.koushikdutta.ion.builder.IonImageViewRequestPostLoadBuilder;
import com.koushikdutta.ion.builder.IonImageViewRequestPreLoadBuilder;
import com.koushikdutta.ion.builder.IonMutableBitmapRequestBuilder;
import com.koushikdutta.ion.builder.IonMutableBitmapRequestPostLoadBuilder;

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

    class BitmapHandler {
        String key;

        public BitmapHandler(String key) {
            this.key = key;
        }

        void report(final Exception e, final Bitmap result) {
            AsyncServer.post(builder.handler, new Runnable() {
                @Override
                public void run() {
                    if (result != null)
                        ion.bitmapCache.put(key, result);

                    final ArrayList<FutureCallback<Bitmap>> callbacks = ion.bitmapsPending.remove(key);
                    if (e == null && result == null)
                        return;
                    if (callbacks == null || callbacks.size() == 0)
                        return;

                    for (FutureCallback<Bitmap> callback: callbacks) {
                        callback.onCompleted(e, result);
                    }
                }
            });
        }
    }

    class ByteBufferListToBitmap extends BitmapHandler implements FutureCallback<ByteBufferList> {
        public ByteBufferListToBitmap(String urlKey) {
            super(urlKey);
        }

        @Override
        public void onCompleted(Exception e, ByteBufferList result) {
            if (e != null) {
                report(e, null);
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
                        report(null, bmp);
                    }
                    catch (Exception e) {
                        report(e, null);
                    }
                }
            });
        }
    }

    class BitmapToBitmap extends BitmapHandler implements FutureCallback<Bitmap> {
        public BitmapToBitmap(String transformKey) {
            super(transformKey);
        }

        @Override
        public void onCompleted(Exception e, final Bitmap result) {
            if (e != null) {
                report(e, null);
                return;
            }

            ion.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap tmpBitmap = result;
                        for (Transform transform : transforms) {
                            builder.request.logd("applying transform: " + transform.getKey());
                            tmpBitmap = transform.transform(tmpBitmap);
                        }
                        report(null, tmpBitmap);
                    }
                    catch (Exception e) {
                        report(e, null);
                    }
                }
            });
        }
    }

    BitmapDrawable getBitmapDrawable(Bitmap bitmap) {
        return new BitmapDrawable(builder.context.get().getResources(), bitmap);
    }

    @Override
    public Future<Bitmap> intoImageView(ImageView imageView) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();

        // the future to return to the caller
        final SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();

        if (imageView != null)
            ion.pendingViews.remove(imageView);
        
        // no url? just set a placeholder and bail
        if (builder.request == null) {
            setPlaceholder(imageView);
            ret.setComplete((Bitmap)null);
            return ret;
        }

        final String urlKey = builder.request.getUri().toString();

        // determine the key for this bitmap after all transformations
        String tmpKey = urlKey;
        if (transforms != null) {
            for (Transform transform : transforms) {
                tmpKey += transform.getKey();
            }
        }
        final String transformKey = tmpKey;

        // see if this request can be fulfilled from the cache
        Bitmap bitmap = builder.ion.bitmapCache.get(transformKey);
        if (bitmap != null) {
            setImageView(imageView, getBitmapDrawable(bitmap));
            doAnimation(imageView, null);
            ret.setComplete(bitmap);
            return ret;
        }

        // need to either load/transform this, so set up the loading images, and mark this view
        // as in progress.
        if (imageView != null)
            ion.pendingViews.put(imageView, transformKey);
        setPlaceholder(imageView);

        // find/create the future for this download.
        if (!ion.bitmapsPending.contains(urlKey)) {
            builder.execute(new ByteBufferListParser()).setCallback(new ByteBufferListToBitmap(urlKey));
        }

        // if the transform key and url key aren't the same, and the transform isn't already queue, queue it
        if (!TextUtils.equals(urlKey, transformKey) && !ion.bitmapsPending.contains(transformKey)) {
            ion.bitmapsPending.add(urlKey, new BitmapToBitmap(transformKey));
        }

        // note whether an ImageView was present during invocation, as
        // only a weak reference is held from here on out.
        final WeakReference<ImageView> iv = new WeakReference<ImageView>(imageView);

        // get a child future that can be used to set the ImageView once the drawable is ready
        ion.bitmapsPending.add(transformKey, new FutureCallback<Bitmap>() {
            @Override
            public void onCompleted(final Exception e, Bitmap source) {
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

                BitmapDrawable result = getBitmapDrawable(source);
                if (imageView != null) {
                    imageView.setImageDrawable(result);
                    doAnimation(imageView, inAnimation);
                }
                ret.setComplete(result.getBitmap());
            }
        });

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
