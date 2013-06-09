package com.koushikdutta.ion;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.koushikdutta.ion.builder.IonMutableBitmapRequestPreLoadBuilder;
import com.koushikdutta.ion.builder.IonMutableBitmapRequestPostLoadBuilder;

/**
 * Created by koush on 5/23/13.
 */
class IonBitmapRequestBuilder implements IonMutableBitmapRequestPreLoadBuilder, IonMutableBitmapRequestPostLoadBuilder, IonImageViewRequestPreLoadBuilder, IonImageViewRequestPostLoadBuilder {
    IonRequestBuilder builder;
    Ion ion;

    @Override
    public Future<ImageView> load(String uri) {
        builder.load(uri);
        return intoImageView(imageViewPostRef.get());
    }

    @Override
    public Future<ImageView> load(String method, String url) {
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

//            builder.request.logd("Image file size: " + result.remaining());
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
//                            builder.request.logd("applying transform: " + transform.getKey());
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

    String bitmapKey;
    Bitmap execute() {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        assert builder.uri != null;

        // determine the key for this bitmap after all transformations
        bitmapKey = builder.uri;
        if (transforms != null) {
            for (Transform transform : transforms) {
                bitmapKey += transform.getKey();
            }
        }

        // see if this request can be fulfilled from the cache
        Bitmap bitmap = builder.ion.bitmapCache.get(bitmapKey);
        if (bitmap != null) {
            return bitmap;
        }

        // find/create the future for this download.
        if (!ion.bitmapsPending.contains(builder.uri)) {
            builder.execute(new ByteBufferListParser()).setCallback(new ByteBufferListToBitmap(builder.uri));
        }

        // if the transform key and uri key aren't the same, and the transform isn't already queue, queue it
        if (!TextUtils.equals(builder.uri, bitmapKey) && !ion.bitmapsPending.contains(bitmapKey)) {
            ion.bitmapsPending.add(builder.uri, new BitmapToBitmap(bitmapKey));
        }

        return null;
    }

    private static final SimpleFuture<ImageView> FUTURE_IMAGEVIEW_NULL_URI = new SimpleFuture<ImageView>() {
        {
            setComplete(new NullPointerException("uri"));
        }
    };

    private static final SimpleFuture<Bitmap> FUTURE_BITMAP_NULL_URI = new SimpleFuture<Bitmap>() {
        {
            setComplete(new NullPointerException("uri"));
        }
    };

    void setIonDrawable(ImageView imageView, Bitmap bitmap) {
        Drawable current = imageView.getDrawable();

        // invalidate self doesn't seem to trigger the dimension check to be called by imageview.
        // are drawable dimensions supposed to be immutable?
        imageView.setImageDrawable(null);

        IonDrawable ret;
        if (current == null || !(current instanceof IonDrawable)) {
            ret = new IonDrawable(imageView.getResources(), null);
        }
        else {
            ret = (IonDrawable)current;
            ret.setDensity(imageView.getResources().getDisplayMetrics().densityDpi);
        }

        ret.setBitmap(bitmap);
        ret.setScaleMode(scaleMode);
        if (scaleMode == ScaleMode.CenterCrop)
            ret.setIntrinsicDimensions(imageView.getMeasuredWidth(), imageView.getMeasuredHeight());

        imageView.setImageDrawable(ret);
    }

    SimpleFuture<ImageView> imageViewFuture;
    @Override
    public Future<ImageView> intoImageView(ImageView imageView) {
        if (imageView == null)
            throw new IllegalArgumentException("imageView");
        assert Thread.currentThread() == Looper.getMainLooper().getThread();

        imageView.setTag(this);

        if (imageViewFuture == null)
            imageViewFuture = new SimpleFuture<ImageView>();
        else
            imageViewFuture.reset();

        // no uri? just set a placeholder and bail
        if (builder.uri == null) {
            setPlaceholder(imageView);
            bitmapKey = null;
            return FUTURE_IMAGEVIEW_NULL_URI;
        }

        // execute the request, see if we get a bitmap from cache.
        Bitmap bitmap = execute();
        // note what this imageview is attached to
        if (bitmap != null) {
            setIonDrawable(imageView, bitmap);
            doAnimation(imageView, null, 0);
            imageViewFuture.setComplete(imageView);
            return imageViewFuture;
        }
        final String loadingBitmapKey = bitmapKey;

        // set the placeholder since we're loading
        setPlaceholder(imageView);

        // note whether an ImageView was present during invocation, as
        // only a weak reference is held from here on out.
        final WeakReference<ImageView> iv;
        if (imageViewPostRef != null)
            iv = imageViewPostRef;
        else
            iv = new WeakReference<ImageView>(imageView);

        // get a child future that can be used to set the ImageView once the drawable is ready
        ion.bitmapsPending.add(loadingBitmapKey, new FutureCallback<Bitmap>() {
            @Override
            public void onCompleted(final Exception e, Bitmap source) {
                assert Thread.currentThread() == Looper.getMainLooper().getThread();

                // see if the imageview is still alive and cares about this result
                ImageView imageView = iv.get();
                if (imageView == null)
                    return;

                // grab the metadata associated with the imageview
                if (imageView.getTag() != IonBitmapRequestBuilder.this)
                    return;

                // see if the ImageView is still waiting for the same transform key as before
                if (!TextUtils.equals(loadingBitmapKey, IonBitmapRequestBuilder.this.bitmapKey))
                    return;

                if (e != null) {
                    setErrorImage(imageView);
                    imageViewFuture.setComplete(e);
                    return;
                }

                setIonDrawable(imageView, source);
                doAnimation(imageView, inAnimation, inAnimationResource);
                imageViewFuture.setComplete(imageView);
            }
        });

        return imageViewFuture;
    }

    @Override
    public Future<Bitmap> asBitmap() {
        // no uri? just set a placeholder and bail
        if (builder.uri == null) {
            return FUTURE_BITMAP_NULL_URI;
        }

        // see if we get something back synchronously
        Bitmap bitmap = execute();
        if (bitmap != null) {
            SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();
            ret.setComplete(bitmap);
            return ret;
        }

        // we're loading, so let's register for the result.
        TransformFuture<Bitmap, Bitmap> ret = new TransformFuture<Bitmap, Bitmap>() {
            @Override
            protected void transform(Bitmap result) throws Exception {
                setComplete(result);
            }
        };
        ion.bitmapsPending.add(bitmapKey, ret);
        return ret;
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

    int placeholderResource;
    @Override
    public IonBitmapRequestBuilder placeholder(int resourceId) {
        placeholderResource = resourceId;
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

    int errorResource;
    @Override
    public IonBitmapRequestBuilder error(int resourceId) {
        errorResource = resourceId;
        return this;
    }

    private static void setImageView(ImageView imageView, Drawable drawable, int resource) {
        if (imageView == null)
            return;
        if (resource != 0)
            drawable = imageView.getContext().getResources().getDrawable(resource);
        imageView.setImageDrawable(drawable);
    }

    private void setPlaceholder(ImageView imageView) {
        if (imageView == null)
            return;
        setImageView(imageView, placeholderDrawable, placeholderResource);
        doAnimation(imageView, loadAnimation, loadAnimationResource);
    }

    private void setErrorImage(ImageView imageView) {
        if (imageView == null)
            return;
        boolean usingPlaceholder = false;
        Drawable drawable = errorDrawable;
        int resource = errorResource;
        // if we don't have a error drawable, keep the placeholder
        if (drawable == null && resource == 0) {
            drawable = placeholderDrawable;
            resource = placeholderResource;
            usingPlaceholder = true;
        }
        setImageView(imageView, drawable, resource);
        if (!usingPlaceholder)
            doAnimation(imageView, inAnimation, inAnimationResource);
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

    private void doAnimation(ImageView imageView, Animation animation, int animationResource) {
        if (imageView == null)
            return;
        if (animation == null && animationResource != 0)
            animation = AnimationUtils.loadAnimation(imageView.getContext(), animationResource);
        if (animation == null) {
            imageView.setAnimation(null);
            return;
        }

        imageView.startAnimation(animation);
    }

    int loadAnimationResource;
    @Override
    public IonBitmapRequestBuilder animateLoad(int animationResource) {
        loadAnimationResource = animationResource;
        return this;
    }

    int inAnimationResource;
    @Override
    public IonBitmapRequestBuilder animateIn(int animationResource) {
        inAnimationResource = animationResource;
        return this;
    }

    static enum ScaleMode {
        CenterCrop,
        CenterInside
    }

    ScaleMode scaleMode;

    @Override
    public IonBitmapRequestBuilder centerCrop() {
        scaleMode = ScaleMode.CenterCrop;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder centerInside() {
        scaleMode = ScaleMode.CenterInside;
        return this;
    }

    void reset() {
        placeholderDrawable = null;
        placeholderResource = 0;
        errorDrawable = null;
        errorResource = 0;
        ion = null;
        imageViewPostRef = null;
        transforms = null;
        bitmapKey = null;
        inAnimation = null;
        inAnimationResource = 0;
        loadAnimation = null;
        loadAnimationResource = 0;
        scaleMode = ScaleMode.CenterInside;
    }
}
