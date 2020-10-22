package com.koushikdutta.ion;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.builder.AnimateGifMode;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.ImageViewFutureBuilder;
import com.koushikdutta.scratch.Deferred;
import com.koushikdutta.scratch.Promise;
import com.koushikdutta.scratch.PromiseApplyCallback;

/**
 * Created by koush on 7/4/14.
 */
public class IonImageViewRequestBuilder extends IonBitmapRequestBuilder implements Builders.IV.F, ImageViewFutureBuilder {
    Drawable placeholderDrawable;
    int placeholderResource;
    Drawable errorDrawable;
    int errorResource;
    Animation inAnimation;
    Animation loadAnimation;
    int loadAnimationResource;
    int inAnimationResource;
    ContextReference.ImageViewContextReference imageViewPostRef;
    boolean fadeIn = true;
    boolean crossfade;
    BitmapDrawableFactory bitmapDrawableFactory = BitmapDrawableFactory.DEFAULT;

    public IonImageViewRequestBuilder(IonRequestBuilder builder) {
        super(builder);
    }

    public IonImageViewRequestBuilder(Ion ion) {
        super(ion);
    }

    @Override
    protected IonRequestBuilder ensureBuilder() {
        if (builder == null)
            builder = new IonRequestBuilder(ContextReference.fromContext(imageViewPostRef.getContext().getApplicationContext()), ion);
        return builder;
    }

    @Override
    public ImageViewFuture load(String uri) {
        ensureBuilder();
        builder.load(uri);
        return intoImageView(imageViewPostRef.get());
    }

    @Override
    public Promise<ImageView> load(String method, String url) {
        ensureBuilder();
        builder.load(method, url);
        return intoImageView(imageViewPostRef.get());
    }

    IonImageViewRequestBuilder withImageView(ImageView imageView) {
        if (imageViewPostRef == null || imageViewPostRef.get() != imageView)
            imageViewPostRef = new ContextReference.ImageViewContextReference(imageView);
        return this;
    }

    private IonDrawable setIonDrawable(ImageView imageView, BitmapInfo bitmapInfo, ResponseServedFrom servedFrom) {
        IonDrawable ret = IonDrawable.getOrCreateIonDrawable(imageView);
        ret.ion(ion)
                .setBitmap(bitmapInfo, servedFrom);
        return setIonDrawableInternal(ret, imageView);
    }

    private IonDrawable setIonDrawable(ImageView imageView, BitmapRequest bitmapFetcher) {
        IonDrawable ret = IonDrawable.getOrCreateIonDrawable(imageView);
        ret.ion(ion)
                .setBitmapFetcher(bitmapFetcher);
        return setIonDrawableInternal(ret, imageView);
    }

    private IonDrawable setIonDrawableInternal(IonDrawable ret, ImageView imageView) {
        ret
        .setRepeatAnimation(animateGifMode == AnimateGifMode.ANIMATE)
        .setSize(resizeWidth, resizeHeight)
        .setError(errorResource, errorDrawable)
        .setPlaceholder(placeholderResource, placeholderDrawable)
        .setFadeIn(fadeIn || crossfade)
        .setBitmapDrawableFactory(bitmapDrawableFactory)
        .updateLayers();
        imageView.setImageDrawable(ret);
        return ret;
    }

    @Override
    public IonBitmapRequestBuilder fadeIn(boolean fadeIn) {
        this.fadeIn = fadeIn;
        return this;
    }

    @Override
    public ImageViewFuture intoImageView(ImageView imageView) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        if (imageView == null)
            throw new NullPointerException("imageView");

        // no uri? just set a placeholder and bail
        if (builder.uri == null) {
            setIonDrawable(imageView, null, ResponseServedFrom.LOADED_FROM_NETWORK);
            return new ImageViewFuture(imageViewPostRef, Promise.reject(new NullPointerException("uri")));
        }

        withImageView(imageView);

        if (crossfade) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof IonDrawable) {
                IonDrawable ionDrawable = (IonDrawable)drawable;
                drawable = ionDrawable.getCurrentDrawable();
            }
            placeholder(drawable);
        }

        int sampleWidth = resizeWidth;
        int sampleHeight = resizeHeight;
        // see if we need default transforms, or this if the imageview
        // will request the actual size on measure
        if (resizeHeight == 0 && resizeWidth == 0 && !imageView.getAdjustViewBounds()) {
            // set the sample size hints from the current dimensions
            // but don't actually apply a transform.
            // this may be zero, in which case IonDrawable
            // will eventually try again with real dimensions
            // during draw.
            sampleWidth = imageView.getMeasuredWidth();
            sampleHeight = imageView.getMeasuredHeight();
        }
        else {
            addDefaultTransform();
        }

        // executeCache the request, see if we get a bitmap from cache.
        BitmapRequest bitmapFetcher = buildRequest(sampleWidth, sampleHeight);
        BitmapInfo bitmapInfo = ion.bitmapManager.checkCache(bitmapFetcher);

        if (bitmapInfo != null) {
            doAnimation(imageView, null, 0);
            IonDrawable drawable = setIonDrawable(imageView, bitmapInfo, ResponseServedFrom.LOADED_FROM_MEMORY);
            ImageViewBitmapInfo info = new ImageViewBitmapInfo();
            info.exception = bitmapInfo.exception;
            info.imageView = imageView;
            info.info = bitmapInfo;
            ImageViewFuture imageViewFuture = new ImageViewFuture(imageViewPostRef, Promise.resolve(info));
            applyScaleMode(imageView, scaleMode);
            return imageViewFuture;
        }

        IonDrawable drawable = setIonDrawable(imageView, bitmapFetcher);
        doAnimation(imageView, loadAnimation, loadAnimationResource);
        Deferred<BitmapInfo> load = new Deferred<>();

        ContextReference.ImageViewContextReference ref = new ContextReference.ImageViewContextReference(imageView);
        ImageViewCallback callback = new ImageViewCallback();
        callback.ref = ref;
        callback.inAnimation = inAnimation;
        callback.inAnimationResource = inAnimationResource;
        callback.drawable = drawable;
        callback.scaleMode = scaleMode;
        drawable.setDrawLoad(load);

        return new ImageViewFuture(ref, load.getPromise().apply(callback));
    }

    static class ImageViewCallback implements PromiseApplyCallback<ImageViewBitmapInfo, BitmapInfo> {
        ContextReference.ImageViewContextReference ref;
        Animation inAnimation;
        int inAnimationResource;
        IonDrawable drawable;
        ScaleMode scaleMode;

        @Override
        public ImageViewBitmapInfo apply(BitmapInfo bitmapInfo) {
            ImageViewBitmapInfo info = new ImageViewBitmapInfo();
            ImageView imageView = ref.get();
            info.imageView = imageView;
            info.info = bitmapInfo;
            info.exception = bitmapInfo.exception;

            if (info.exception == null)
                applyScaleMode(imageView, scaleMode);

            IonBitmapRequestBuilder.doAnimation(imageView, inAnimation, inAnimationResource);
            imageView.setImageDrawable(null);
            imageView.setImageDrawable(drawable);

            return info;
        }
    }

    public static void applyScaleMode(ImageView imageView, ScaleMode scaleMode) {
        if (scaleMode == null)
            return;
        switch (scaleMode) {
            case CenterCrop:
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case FitCenter:
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                break;
            case CenterInside:
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case FitXY:
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
        }
    }

    private Drawable getImageViewDrawable() {
        ImageView iv = imageViewPostRef.get();
        if (iv == null)
            return null;
        return iv.getDrawable();
    }

    @Override
    public Bitmap getBitmap() {
        Drawable d = getImageViewDrawable();
        if (d == null)
            return null;
        if (d instanceof BitmapDrawable)
            return ((BitmapDrawable)d).getBitmap();
        if (!(d instanceof IonDrawable))
            return null;
        IonDrawable id = (IonDrawable)d;
        d = id.getCurrentDrawable();
        if (d instanceof BitmapDrawable)
            return ((BitmapDrawable)d).getBitmap();
        return null;
    }

    @Override
    public BitmapInfo getBitmapInfo() {
        Drawable d = getImageViewDrawable();
        if (d == null)
            return null;
        if (!(d instanceof IonDrawable))
            return null;
        IonDrawable id = (IonDrawable)d;
        return id.getBitmapInfo();
    }


    @Override
    public IonImageViewRequestBuilder crossfade(boolean crossfade) {
        this.crossfade = crossfade;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder placeholder(Drawable drawable) {
        placeholderDrawable = drawable;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder placeholder(int resourceId) {
        placeholderResource = resourceId;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder error(Drawable drawable) {
        errorDrawable = drawable;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder error(int resourceId) {
        errorResource = resourceId;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder animateIn(Animation in) {
        inAnimation = in;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder animateLoad(Animation load) {
        loadAnimation = load;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder animateLoad(int animationResource) {
        loadAnimationResource = animationResource;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder animateIn(int animationResource) {
        inAnimationResource = animationResource;
        return this;
    }

    @Override
    public IonImageViewRequestBuilder bitmapDrawableFactory(BitmapDrawableFactory bitmapDrawableFactory) {
        this.bitmapDrawableFactory = bitmapDrawableFactory;
        return this;
    }
}
