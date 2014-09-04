package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.builder.AnimateGifMode;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.ImageViewFutureBuilder;
import com.koushikdutta.ion.future.ImageViewFuture;

/**
 * Created by koush on 7/4/14.
 */
public class IonImageViewRequestBuilder extends IonBitmapRequestBuilder implements Builders.IV.F, ImageViewFutureBuilder {
    private static final IonDrawable.ImageViewFutureImpl FUTURE_IMAGEVIEW_NULL_URI = new IonDrawable.ImageViewFutureImpl() {
        {
            setComplete(new NullPointerException("uri"));
        }
    };

    Drawable placeholderDrawable;
    int placeholderResource;
    Drawable errorDrawable;
    int errorResource;
    Animation inAnimation;
    Animation loadAnimation;
    int loadAnimationResource;
    int inAnimationResource;
    ContextReference.ImageViewContextReference imageViewPostRef;

    public IonImageViewRequestBuilder(IonRequestBuilder builder) {
        super(builder);
    }

    public IonImageViewRequestBuilder(Ion ion) {
        super(ion);
    }

    @Override
    void reset() {
        super.reset();
        imageViewPostRef = null;
        placeholderDrawable = null;
        placeholderResource = 0;
        errorDrawable = null;
        errorResource = 0;
        inAnimation = null;
        inAnimationResource = 0;
        loadAnimation = null;
        loadAnimationResource = 0;
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
    public Future<ImageView> load(String method, String url) {
        ensureBuilder();
        builder.load(method, url);
        return intoImageView(imageViewPostRef.get());
    }

    IonImageViewRequestBuilder withImageView(ImageView imageView) {
        imageViewPostRef = new ContextReference.ImageViewContextReference(imageView);
        return this;
    }

    private IonDrawable setIonDrawable(ImageView imageView, BitmapFetcher bitmapFetcher, int loadedFrom) {
        BitmapInfo info = bitmapFetcher.info;
        if (info != null)
            bitmapFetcher = null;

        IonDrawable ret = IonDrawable.getOrCreateIonDrawable(imageView)
        .ion(ion)
        .setBitmap(info, loadedFrom)
        .setBitmapFetcher(bitmapFetcher)
        .setRepeatAnimation(animateGifMode == AnimateGifMode.ANIMATE)
        .setSize(resizeWidth, resizeHeight)
        .setError(errorResource, errorDrawable)
        .setPlaceholder(placeholderResource, placeholderDrawable)
        .setInAnimation(inAnimation, inAnimationResource)
        .setDisableFadeIn(disableFadeIn);
        imageView.setImageDrawable(ret);
        return ret;
    }

    @Override
    public ImageViewFuture intoImageView(ImageView imageView) {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
        if (imageView == null)
            throw new NullPointerException("imageView");

        // no uri? just set a placeholder and bail
        if (builder.uri == null) {
            setIonDrawable(imageView, null, 0).cancel();
            return FUTURE_IMAGEVIEW_NULL_URI;
        }

        withImageView(imageView);

        // executeCache the request, see if we get a bitmap from cache.
        BitmapFetcher bitmapFetcher = executeCache();
        if (bitmapFetcher.info != null) {
            doAnimation(imageView, null, 0);
            IonDrawable drawable = setIonDrawable(imageView, bitmapFetcher, Loader.LoaderEmitter.LOADED_FROM_MEMORY);
            drawable.cancel();
            IonDrawable.ImageViewFutureImpl imageViewFuture = drawable.getFuture();
            imageViewFuture.reset();
            imageViewFuture.setComplete(bitmapFetcher.info.exception, imageView);
            return imageViewFuture;
        }

        IonDrawable drawable = setIonDrawable(imageView, bitmapFetcher, 0);
        doAnimation(imageView, loadAnimation, loadAnimationResource);
        IonDrawable.ImageViewFutureImpl imageViewFuture = drawable.getFuture();
        imageViewFuture.reset();
        drawable.register(ion, bitmapFetcher.bitmapKey);

        return imageViewFuture;
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
    public IonImageViewRequestBuilder crossfade() {
        ImageView iv = imageViewPostRef.get();
        Drawable drawable = iv.getDrawable();
        if (drawable instanceof IonDrawable) {
            IonDrawable ionDrawable = (IonDrawable)drawable;
            drawable = ionDrawable.getCurrentDrawable();
        }
        return placeholder(drawable);
    }

    @Override
    protected void finalizeResize() {
        if (resizeWidth > 0 && resizeHeight > 0)
            return;
        ImageView iv = imageViewPostRef.get();
        ViewGroup.LayoutParams lp = iv.getLayoutParams();
        if (lp == null)
            return;
        if (resizeWidth <= 0 && lp.width > 0)
            resizeWidth = lp.width;
        if (resizeHeight <= 0 && lp.height > 0)
            resizeHeight = lp.height;
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
}
