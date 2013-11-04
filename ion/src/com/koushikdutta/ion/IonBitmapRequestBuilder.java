package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.async.http.libcore.DiskLruCache;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.builder.BitmapFutureBuilder;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.ImageViewFutureBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by koush on 5/23/13.
 */
class IonBitmapRequestBuilder implements Builders.ImageView.F, ImageViewFutureBuilder, BitmapFutureBuilder, Builders.Any.BF {
    IonRequestBuilder builder;
    Ion ion;

    private void ensureBuilder() {
        if (builder == null)
            builder = new IonRequestBuilder(imageViewPostRef.get().getContext(), ion);
    }

    @Override
    public Future<ImageView> load(String uri) {
        ensureBuilder();
        builder.load(uri);
        return intoImageView(imageViewPostRef.get());
    }

    @Override
    public Future<ImageView> load(String method, String url) {
        ensureBuilder();
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

    public IonBitmapRequestBuilder(Ion ion) {
        this.ion = ion;
    }

    ArrayList<Transform> transforms;

    @Override
    public IonBitmapRequestBuilder transform(Transform transform) {
        if (transforms == null)
            transforms = new ArrayList<Transform>();
        transforms.add(transform);
        return this;
    }

    String bitmapKey;
    BitmapInfo execute() {
        final String downloadKey = ResponseCacheMiddleware.toKeyString(builder.uri);
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        assert downloadKey != null;

        if (resizeHeight != 0 || resizeWidth != 0) {
            transform(new DefaultTransform(resizeWidth, resizeHeight, scaleMode));
        }

        // determine the key for this bitmap after all transformations
        bitmapKey = downloadKey;
        boolean hasTransforms = transforms != null && transforms.size() > 0;
        if (hasTransforms) {
            for (Transform transform : transforms) {
                bitmapKey += transform.key();
            }
            bitmapKey = ResponseCacheMiddleware.toKeyString(bitmapKey);
        }

        // see if this request can be fulfilled from the cache
        if (!builder.noCache) {
            BitmapInfo bitmap = builder.ion.bitmapCache.get(bitmapKey);
            if (bitmap != null)
                return bitmap;
        }

        // bitmaps that were transformed are put into the DiskLruCache to prevent
        // subsequent retransformation. See if we can retrieve the bitmap from the disk cache.
        // See BitmapToBitmapInfo for where the cache is populated.
       DiskLruCache diskLruCache = ion.getResponseCache().getDiskLruCache();
        if (!builder.noCache && hasTransforms && diskLruCache.containsKey(bitmapKey)) {
            BitmapToBitmapInfo.getBitmapSnapshot(ion, bitmapKey);
            return null;
        }

        // Perform a download as necessary.
        if (!ion.bitmapsPending.contains(downloadKey)) {
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
            emitterTransform.setCallback(new LoadBitmap(ion, downloadKey, !hasTransforms, resizeWidth, resizeHeight, emitterTransform));
        }

        // if there's a transform, do it
        if (!hasTransforms)
            return null;

        // verify this transform isn't already pending
        // make sure that the parent download isn't cancelled (empty list)
        // and also make sure there are waiters for this transformed bitmap
        if (!ion.bitmapsPending.contains(downloadKey) || !ion.bitmapsPending.contains(bitmapKey)) {
            ion.bitmapsPending.add(downloadKey, new BitmapToBitmapInfo(ion, bitmapKey, downloadKey, transforms));
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

    private IonDrawable setIonDrawable(ImageView imageView, BitmapInfo info, int loadedFrom) {
        IonDrawable ret = IonDrawable.getOrCreateIonDrawable(imageView);
        ret.setBitmap(info, loadedFrom);
        ret.setSize(resizeWidth, resizeHeight);
        ret.setError(errorResource, errorDrawable);
        ret.setPlaceholder(placeholderResource, placeholderDrawable);
        ret.setInAnimation(inAnimation, inAnimationResource);
        ret.setDisableFadeIn(disableFadeIn);
        imageView.setImageDrawable(ret);
        return ret;
    }

    @Override
    public Future<ImageView> intoImageView(ImageView imageView) {
        if (imageView == null)
            throw new IllegalArgumentException("imageView");
        assert Thread.currentThread() == Looper.getMainLooper().getThread();

        // no uri? just set a placeholder and bail
        if (builder.uri == null) {
            bitmapKey = null;
            setIonDrawable(imageView, null, 0).cancel();
            return FUTURE_IMAGEVIEW_NULL_URI;
        }

        // execute the request, see if we get a bitmap from cache.
        BitmapInfo info = execute();
        if (info != null) {
            doAnimation(imageView, null, 0);
            IonDrawable drawable = setIonDrawable(imageView, info, Loader.LoaderEmitter.LOADED_FROM_MEMORY);
            drawable.cancel();
            SimpleFuture<ImageView> imageViewFuture = drawable.getFuture();
            imageViewFuture.reset();
            imageViewFuture.setComplete(imageView);
            return imageViewFuture;
        }

        IonDrawable drawable = setIonDrawable(imageView, null, 0);
        doAnimation(imageView, loadAnimation, loadAnimationResource);
        SimpleFuture<ImageView> imageViewFuture = drawable.getFuture();
        imageViewFuture.reset();

        drawable.register(ion, bitmapKey);

        return imageViewFuture;
    }

    private static class BitmapInfoToBitmap extends TransformFuture<Bitmap, BitmapInfo> {
        @Override
        protected void transform(BitmapInfo result) throws Exception {
            setComplete(result.bitmaps[0]);
        }
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
            ret.setComplete(info.bitmaps[0]);
            return ret;
        }

        // we're loading, so let's register for the result.
        BitmapInfoToBitmap ret = new BitmapInfoToBitmap();
        ion.bitmapsPending.add(bitmapKey, ret);
        return ret;
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

    static void doAnimation(ImageView imageView, Animation animation, int animationResource) {
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

    private boolean disableFadeIn;

    @Override
    public IonBitmapRequestBuilder disableFadeIn() {
        this.disableFadeIn = true;
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
        scaleMode = ScaleMode.FitXY;
        resizeWidth = 0;
        resizeHeight = 0;
        disableFadeIn = false;
        builder = null;
    }
}
