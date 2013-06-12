package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
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
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.builder.BitmapFutureBuilder;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.ImageViewFutureBuilder;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by koush on 5/23/13.
 */
class IonBitmapRequestBuilder implements Builders.ImageView.F, ImageViewFutureBuilder, BitmapFutureBuilder {
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

    class BitmapCallback {
        String key;
        IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform;

        public BitmapCallback(String key, boolean put, IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform) {
            this.key = key;
            this.put = put;
            this.emitterTransform = emitterTransform;
        }

        boolean put;
        boolean put() {
            return put;
        }

        void report(final Exception e, final BitmapInfo result) {
            AsyncServer.post(IonRequestBuilder.mainHandler, new Runnable() {
                @Override
                public void run() {
                    if (result != null && put()) {
                        ion.bitmapCache.put(result);
                    }

                    final ArrayList<FutureCallback<BitmapInfo>> callbacks = ion.bitmapsPending.remove(key);
                    if (e == null && result == null)
                        return;
                    if (callbacks == null || callbacks.size() == 0)
                        return;

                    for (FutureCallback<BitmapInfo> callback: callbacks) {
                        callback.onCompleted(e, result);
                    }
                }
            });
        }
    }

    class LoadBitmap extends BitmapCallback implements FutureCallback<ByteBufferList> {
        public LoadBitmap(String urlKey, boolean put, IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform) {
            super(urlKey, put, emitterTransform);
        }

        @Override
        public void onCompleted(Exception e, final ByteBufferList result) {
            if (e != null) {
                report(e, null);
                return;
            }

//            builder.request.logd("Image file size: " + result.remaining());

            ion.getServer().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        final ByteArrayInputStream bin = new ByteArrayInputStream(result.getAllByteArray());
                        BitmapInfo info = ion.bitmapCache.loadBitmapFromStream(bin, key, emitterTransform.loadedFrom());
                        if (info == null)
                            throw new Exception("bitmap failed to load");
                        report(null, info);
                    }
                    catch (Exception e) {
                        report(e, null);
                    }
                }
            });
        }
    }

    class BitmapToBitmap extends BitmapCallback implements FutureCallback<BitmapInfo> {
        public BitmapToBitmap(String transformKey, IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform) {
            super(transformKey, true, emitterTransform);
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
                    try {
                        Bitmap tmpBitmap = result.bitmap;
                        for (Transform transform : transforms) {
//                            builder.request.logd("applying transform: " + transform.key());
                            tmpBitmap = transform.transform(tmpBitmap);
                        }
                        BitmapInfo info = new BitmapInfo();
                        info.loadedFrom = result.loadedFrom;
                        info.bitmap = tmpBitmap;
                        info.key = key;
                        report(null, info);
                    }
                    catch (Exception e) {
                        report(e, null);
                    }
                }
            });
        }
    }

    String bitmapKey;
    BitmapInfo execute() {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        assert builder.uri != null;

        if (resizeHeight != 0 || resizeWidth != 0) {
            transform(new DefaultTransform(resizeWidth, resizeHeight, scaleMode));
            resizeWidth = 0;
            resizeHeight = 0;
            scaleMode = null;
        }

        // determine the key for this bitmap after all transformations
        bitmapKey = builder.uri;
        boolean hasTransforms = transforms != null && transforms.size() > 0;
        if (hasTransforms) {
            for (Transform transform : transforms) {
                bitmapKey += transform.key();
            }
        }

        // see if this request can be fulfilled from the cache
        BitmapInfo bitmap = builder.ion.bitmapCache.get(bitmapKey);
        if (bitmap != null) {
            return bitmap;
        }

        // find/create the future for this download.
        IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform = null;
        if (!ion.bitmapsPending.contains(builder.uri)) {
            builder.setHandler(null);
            emitterTransform = builder.execute(new ByteBufferListParser());
            emitterTransform.setCallback(new LoadBitmap(builder.uri, !hasTransforms, emitterTransform));
        }

        // if there's a transform, do it
        if (!hasTransforms)
            return null;

        ion.bitmapsPending.add(builder.uri, new BitmapToBitmap(bitmapKey, emitterTransform));

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

    IonDrawable getOrCreateIonDrawable(ImageView imageView) {
        Drawable current = imageView.getDrawable();
        IonDrawable ret;
        if (current == null || !(current instanceof IonDrawable)) {
            ret = new IonDrawable();
        }
        else {
            ret = (IonDrawable)current;
        }
        // invalidate self doesn't seem to trigger the dimension check to be called by imageview.
        // are drawable dimensions supposed to be immutable?
        imageView.setImageDrawable(null);
        return ret;
    }

    void setIonDrawable(ImageView imageView, Drawable drawable) {
        IonDrawable ret = getOrCreateIonDrawable(imageView);

        int w = resizeWidth;
        int h = resizeHeight;
        if ((w == 0 || h == 0) && drawable != null) {
            w = drawable.getIntrinsicWidth();
            h = drawable.getIntrinsicHeight();
        }

//        ret.setPlaceholder(drawable, w, h);
//        imageView.setImageDrawable(ret);
        imageView.setImageDrawable(drawable);
    }

    void setIonDrawable(ImageView imageView, BitmapInfo info) {
        Bitmap bitmap = info.bitmap;
        IonDrawable ret = getOrCreateIonDrawable(imageView);

        int w = resizeWidth;
        int h = resizeHeight;
        if (w == 0 || h == 0) {
            int density = imageView.getResources().getDisplayMetrics().densityDpi;
            w = bitmap.getScaledWidth(density);
            h = bitmap.getScaledHeight(density);
        }

        ret.setBitmap(info, w, h);
        imageView.setImageDrawable(ret);
    }

    int executeCount;
    SimpleFuture<ImageView> imageViewFuture;
    @Override
    public Future<ImageView> intoImageView(ImageView imageView) {
        if (imageView == null)
            throw new IllegalArgumentException("imageView");
        assert Thread.currentThread() == Looper.getMainLooper().getThread();

        // tag this load with an id so that when the execute completes,
        // we know if the image view is still serving the same request.
        executeCount++;
        final int loadingExecuteCount = executeCount;

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
        BitmapInfo info = execute();
        if (info != null) {
            setIonDrawable(imageView, info);
            doAnimation(imageView, null, 0);
            imageViewFuture.setComplete(imageView);
            return imageViewFuture;
        }

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
        ion.bitmapsPending.add(bitmapKey, new FutureCallback<BitmapInfo>() {
            @Override
            public void onCompleted(final Exception e, BitmapInfo source) {
                assert Thread.currentThread() == Looper.getMainLooper().getThread();

                // see if the imageview is still alive and cares about this result
                ImageView imageView = iv.get();
                if (imageView == null)
                    return;

                // grab the metadata associated with the imageview
                if (imageView.getTag() != IonBitmapRequestBuilder.this)
                    return;

                // see if the ImageView is still waiting for the same transform key as before
                if (loadingExecuteCount != executeCount)
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
        BitmapInfo info = execute();
        if (info != null) {
            SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();
            ret.setComplete(info.bitmap);
            return ret;
        }

        // we're loading, so let's register for the result.
        TransformFuture<Bitmap, BitmapInfo> ret = new TransformFuture<Bitmap, BitmapInfo>() {
            @Override
            protected void transform(BitmapInfo result) throws Exception {
                setComplete(result.bitmap);
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

    private void setImageView(ImageView imageView, Drawable drawable, int resource) {
        if (imageView == null)
            return;
        if (resource != 0)
            drawable = imageView.getContext().getResources().getDrawable(resource);
//        imageView.setImageDrawable(drawable);
        setIonDrawable(imageView, drawable);
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
        FitXY,
        CenterCrop,
        CenterInside
    }

    ScaleMode scaleMode = ScaleMode.FitXY;
    @Override
    public IonBitmapRequestBuilder centerCrop() {
        if (resizeWidth == 0 || resizeHeight == 0)
            throw new IllegalStateException("must call resize first");
        scaleMode = ScaleMode.CenterCrop;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder centerInside() {
        if (resizeWidth == 0 || resizeHeight == 0)
            throw new IllegalStateException("must call resize first");
        scaleMode = ScaleMode.CenterInside;
        return this;
    }

    int resizeWidth;
    int resizeHeight;

    @Override
    public IonBitmapRequestBuilder resize(int width, int height) {
        resizeWidth = width;
        resizeHeight = height;
        return this;
    }

    static class DefaultTransform implements Transform {
        ScaleMode scaleMode;
        int resizeWidth;
        int resizeHeight;

        public DefaultTransform(int width, int height, ScaleMode scaleMode) {
            resizeWidth = width;
            resizeHeight = height;
            this.scaleMode = scaleMode;
        }

        @Override
        public Bitmap transform(Bitmap b) {
            Bitmap ret = Bitmap.createBitmap(resizeWidth, resizeHeight, b.getConfig());
            Canvas canvas = new Canvas(ret);

            int transx = b.getWidth() >> 1;
            int transy = b.getHeight() >> 1;

//            canvas.translate(transx, transy);

            float xratio = (float)resizeWidth / (float)b.getWidth();
            float yratio = (float)resizeHeight / (float)b.getHeight();
            if (scaleMode != ScaleMode.FitXY) {
                float ratio;
                if (scaleMode == ScaleMode.CenterCrop)
                    ratio = Math.max(xratio, yratio);
                else
                    ratio = Math.min(xratio, yratio);

                xratio = ratio;
                yratio = ratio;
            }

            canvas.scale(xratio, yratio);
            canvas.drawBitmap(b, 0, 0, null);

            return ret;
        }

        @Override
        public String key() {
            return scaleMode.name() + resizeWidth  + "x" + resizeHeight;
        }
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
        scaleMode = ScaleMode.FitXY;
        resizeWidth = 0;
        resizeHeight = 0;
    }
}
