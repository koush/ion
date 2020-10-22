package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.LocallyCachedStatus;
import com.koushikdutta.ion.bitmap.PostProcess;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.builder.AnimateGifMode;
import com.koushikdutta.ion.builder.BitmapFutureBuilder;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.IonPromise;
import com.koushikdutta.ion.util.ByteBufferListParser;
import com.koushikdutta.scratch.LooperKt;
import com.koushikdutta.scratch.Promise;
import com.koushikdutta.scratch.event.FileStore;

import java.util.ArrayList;

import kotlin.NotImplementedError;

/**
 * Created by koush on 5/23/13.
 */
abstract class IonBitmapRequestBuilder implements BitmapFutureBuilder, Builders.Any.BF {
    IonRequestBuilder builder;
    Ion ion;
    ArrayList<Transform> transforms;
    ScaleMode scaleMode;
    int resizeWidth;
    int resizeHeight;
    AnimateGifMode animateGifMode = AnimateGifMode.ANIMATE;
    boolean deepZoom;
    ArrayList<PostProcess> postProcess;

    public IonBitmapRequestBuilder(IonRequestBuilder builder) {
        this.builder = builder;
        ion = builder.ion;
    }

    public IonBitmapRequestBuilder(Ion ion) {
        this.ion = ion;
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

    protected IonRequestBuilder ensureBuilder() {
        return builder;
    }

    @Override
    public IonBitmapRequestBuilder transform(Transform transform) {
        if (transform == null)
            return this;
        if (transforms == null)
            transforms = new ArrayList<Transform>();
        transforms.add(transform);
        return this;
    }

    @Override
    public IonBitmapRequestBuilder postProcess(PostProcess postProcess) {
        if (this.postProcess == null)
            this.postProcess = new ArrayList<>();
        this.postProcess.add(postProcess);
        return transform(new PostProcessNullTransform(postProcess.key()));
    }

    static class PostProcessNullTransform implements Transform {
        String key;
        public PostProcessNullTransform(String key) {
            this.key = key;
        }

        @Override
        public Bitmap transform(Bitmap b) {
            return b;
        }

        @Override
        public String key() {
            return key;
        }
    }

    public void addDefaultTransform() {
        if (resizeHeight > 0 || resizeWidth > 0) {
            if (transforms == null)
                transforms = new ArrayList<Transform>();
            transforms.add(0, new DefaultTransform(resizeWidth, resizeHeight, scaleMode));
        }
        else if (scaleMode != null) {
            throw new IllegalStateException("Must call resize when using " + scaleMode);
        }
    }

    private String computeDecodeKey() {
        return BitmapRequest.computeDecodeKey(builder, resizeWidth, resizeHeight, animateGifMode != AnimateGifMode.NO_ANIMATE, deepZoom);
    }

    public String computeBitmapKey(String decodeKey) {
        return BitmapRequest.computeBitmapKey(decodeKey, transforms);
    }

    @Override
    public LocallyCachedStatus isLocallyCached() {
        if (builder.noCache || deepZoom)
            return LocallyCachedStatus.NOT_CACHED;
        final String decodeKey = computeDecodeKey();
        addDefaultTransform();
        String bitmapKey = computeBitmapKey(decodeKey);
        BitmapInfo info = builder.ion.bitmapCache.get(bitmapKey);
        // memory cache
        if (info != null && info.exception == null)
            return LocallyCachedStatus.CACHED;
        FileStore fileCache = ion.getCache();
        if (hasTransforms() && fileCache.exists(bitmapKey))
            return LocallyCachedStatus.CACHED;
        if (fileCache.exists(decodeKey))
            return LocallyCachedStatus.MAYBE_CACHED;
        return LocallyCachedStatus.NOT_CACHED;
    }


    @Override
    public void removeCachedBitmap() {
        final String decodeKey = computeDecodeKey();
        addDefaultTransform();
        String bitmapKey = computeBitmapKey(decodeKey);
        FileStore fileCache = ion.getCache();
        fileCache.removeAsync(decodeKey);
        fileCache.removeAsync(bitmapKey);
        builder.ion.bitmapCache.remove(bitmapKey);
        builder.ion.bitmapCache.remove(decodeKey);
    }

    @Override
    public BitmapInfo asCachedBitmap() {
        final String decodeKey = computeDecodeKey();
        addDefaultTransform();
        String bitmapKey = computeBitmapKey(decodeKey);
        return builder.ion.bitmapCache.get(bitmapKey);
    }

    BitmapRequest buildRequest() {
        return buildRequest(resizeWidth, resizeHeight);
    }

    BitmapRequest buildRequest(int sampleWidth, int sampleHeight) {
        BitmapRequest ret = new BitmapRequest();
        ret.sampleWidth = sampleWidth;
        ret.sampleHeight = sampleHeight;
        ret.transforms = transforms;
        ret.animateGif = animateGifMode != AnimateGifMode.NO_ANIMATE;
        ret.deepZoom = deepZoom;
        ret.postProcess = postProcess;
        ret.executor = builder.prepareExecute(new ByteBufferListParser("image/*"), null);

        return ret;
    }

    @Override
    public IonPromise<Bitmap> asBitmap() {
        BitmapRequest request = buildRequest();
        return new IonPromise<>(builder.handler != null ? LooperKt.createAsyncAffinity(builder.handler) : null, ion.bitmapManager.request(request)
        .apply(bitmapInfo -> {
            if (bitmapInfo.exception != null)
                throw bitmapInfo.exception;
            return bitmapInfo.bitmap;
        }));
    }

    private void checkNoTransforms(String name) {
        if (hasTransforms()) {
            throw new IllegalStateException("Can't apply " + name + " after transform has been called."
             + name + " is applied to the original resized bitmap.");
        }
    }

    @Override
    public IonBitmapRequestBuilder centerCrop() {
        checkNoTransforms("centerCrop");
        scaleMode = ScaleMode.CenterCrop;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder fitXY() {
        checkNoTransforms("fitXY");
        scaleMode = ScaleMode.FitXY;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder fitCenter() {
        checkNoTransforms("fitCenter");
        scaleMode = ScaleMode.FitCenter;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder centerInside() {
        checkNoTransforms("centerInside");
        scaleMode = ScaleMode.CenterInside;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder resize(int width, int height) {
        // TODO: prevent multiple calls to resize and friends?
        if (hasTransforms()) {
            throw new IllegalStateException("Can't apply resize after transform has been called." +
            "resize is applied to the original bitmap.");
        }
        if (deepZoom)
            throw new IllegalStateException("Can not resize with deepZoom.");
        resizeWidth = width;
        resizeHeight = height;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder resizeWidth(int width) {
        return resize(width, 0);
    }

    @Override
    public IonBitmapRequestBuilder resizeHeight(int height) {
        return resize(0, height);
    }

	public IonBitmapRequestBuilder smartSize(boolean smartSize) {
        //don't want to disable device resize if user has already resized the Bitmap.
        if (resizeWidth > 0 || resizeHeight > 0)
            throw new IllegalStateException("Can't set smart size after resize has been called.");

        if (deepZoom)
            throw new IllegalStateException("Can not smartSize with deepZoom.");

        if (!smartSize) {
			resizeWidth = -1;
			resizeHeight = -1;
		}
        else {
            resizeWidth = 0;
            resizeHeight = 0;
        }
		return this;
	}

    @Override
    public IonBitmapRequestBuilder animateGif(AnimateGifMode mode) {
        this.animateGifMode = mode;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder deepZoom() {
        this.deepZoom = true;
        if (resizeWidth > 0 || resizeHeight > 0)
            throw new IllegalStateException("Can't deepZoom with resize.");
        if (hasTransforms())
            throw new IllegalStateException("Can't deepZoom with transforms.");
        resizeWidth = 0;
        resizeHeight = 0;
        return this;
    }

    boolean hasTransforms() {
        return transforms != null && transforms.size() > 0;
    }
}
