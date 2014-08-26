package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.os.Build;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.util.FileCache;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.LocallyCachedStatus;
import com.koushikdutta.ion.bitmap.PostProcess;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.builder.BitmapFutureBuilder;
import com.koushikdutta.ion.builder.Builders;

import java.util.ArrayList;

/**
 * Created by koush on 5/23/13.
 */
abstract class IonBitmapRequestBuilder implements BitmapFutureBuilder, Builders.Any.BF {
    private static final SimpleFuture<Bitmap> FUTURE_BITMAP_NULL_URI = new SimpleFuture<Bitmap>() {
        {
            setComplete(new NullPointerException("uri"));
        }
    };

    IonRequestBuilder builder;
    Ion ion;
    ArrayList<Transform> transforms;
    ScaleMode scaleMode = ScaleMode.FitXY;
    int resizeWidth;
    int resizeHeight;
    boolean disableFadeIn;
    AnimateGifMode animateGifMode = AnimateGifMode.ANIMATE;
    boolean deepZoom;
    ArrayList<PostProcess> postProcess;

    void reset() {
        ion = null;
        transforms = null;
        scaleMode = ScaleMode.FitXY;
        resizeWidth = 0;
        resizeHeight = 0;
        disableFadeIn = false;
        animateGifMode = AnimateGifMode.ANIMATE;
        builder = null;
        deepZoom = false;
        postProcess = null;
    }

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
            this.postProcess = new ArrayList<PostProcess>();
        this.postProcess.add(postProcess);
        return transform(new TransformBitmap.PostProcessNullTransform(postProcess.key()));
    }

    private String computeDownloadKey() {
        String downloadKey = builder.uri;
        // although a gif is always same download, the decode (non/animated) result may different
        if (animateGifMode != AnimateGifMode.ANIMATE)
            downloadKey += ":animateGif=" + animateGifMode.name();
        if (deepZoom)
            downloadKey += ":deepZoom";
        return FileCache.toKeyString(downloadKey);
    }

    protected void finalizeResize() {
    }

    public String computeBitmapKey(String downloadKey) {
        assert downloadKey != null;

        if (resizeHeight > 0 || resizeWidth > 0 || scaleMode != ScaleMode.FitXY) {
            finalizeResize();
            if (resizeWidth < 0 && resizeHeight < 0) {
                throw new IllegalStateException("must provide valid resize dimensions if using" +
                "centerCrop or centerInside\n\n" +
                "Valid values: (<=0, Y) or (X, <=0), or (X, Y), where:\n" +
                "X and Y are greater than 0. Specifying <=0 values will scale proportionately.\n" +
                "Values can also be implied through layout parameters.");
            }
            if (transforms == null)
                transforms = new ArrayList<Transform>();
            transforms.add(0, new DefaultTransform(resizeWidth, resizeHeight, scaleMode));
        }

        // determine the key for this bitmap after all transformations
        String bitmapKey = downloadKey;
        if (hasTransforms()) {
            for (Transform transform : transforms) {
                bitmapKey += transform.key();
            }
            bitmapKey = FileCache.toKeyString(bitmapKey);
        }

        return bitmapKey;
    }

    @Override
    public LocallyCachedStatus isLocallyCached() {
        if (builder.noCache || deepZoom)
            return LocallyCachedStatus.NOT_CACHED;
        final String downloadKey = computeDownloadKey();
        String bitmapKey = computeBitmapKey(downloadKey);
        BitmapInfo info = builder.ion.bitmapCache.get(bitmapKey);
        // memory cache
        if (info != null && info.bitmaps != null)
            return LocallyCachedStatus.CACHED;
        FileCache fileCache = ion.responseCache.getFileCache();
        if (hasTransforms() && fileCache.exists(bitmapKey))
            return LocallyCachedStatus.CACHED;
        if (fileCache.exists(downloadKey))
            return LocallyCachedStatus.MAYBE_CACHED;
        return LocallyCachedStatus.NOT_CACHED;
    }

    @Override
    public BitmapInfo asCachedBitmap() {
        final String downloadKey = computeDownloadKey();
        String bitmapKey = computeBitmapKey(downloadKey);
        return builder.ion.bitmapCache.get(bitmapKey);
    }

    BitmapFetcher executeCache() {
        final String downloadKey = computeDownloadKey();
        String bitmapKey = computeBitmapKey(downloadKey);

        // TODO: eliminate this allocation?
        BitmapFetcher ret = new BitmapFetcher();
        ret.downloadKey = downloadKey;
        ret.bitmapKey = bitmapKey;
        ret.hasTransforms = hasTransforms();
        ret.resizeWidth = resizeWidth;
        ret.resizeHeight = resizeHeight;
        ret.builder = builder;
        ret.transforms = transforms;
        ret.animateGif = animateGifMode != AnimateGifMode.NO_ANIMATE;
        ret.deepZoom = deepZoom;
        ret.postProcess = postProcess;

        // see if this request can be fulfilled from the cache
        if (!builder.noCache) {
            BitmapInfo bitmap = builder.ion.bitmapCache.get(bitmapKey);
            if (bitmap != null) {
                ret.info = bitmap;
                return ret;
            }
        }

        return ret;
    }

    @Override
    public Future<Bitmap> asBitmap() {
        if (builder.uri == null) {
            return FUTURE_BITMAP_NULL_URI;
        }

        // see if we get something back synchronously
        final BitmapFetcher bitmapFetcher = executeCache();
        if (bitmapFetcher.info != null) {
            SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();
            Bitmap bitmap = bitmapFetcher.info.bitmaps == null ? null : bitmapFetcher.info.bitmaps[0];
            ret.setComplete(bitmapFetcher.info.exception, bitmap);
            return ret;
        }

        final BitmapInfoToBitmap ret = new BitmapInfoToBitmap(builder.contextReference);
        AsyncServer.post(Ion.mainHandler, new Runnable() {
            @Override
            public void run() {
                bitmapFetcher.execute();
                // we're loading, so let's register for the result.
                ion.bitmapsPending.add(bitmapFetcher.bitmapKey, ret);
            }
        });
        return ret;
    }

    @Override
    public IonBitmapRequestBuilder centerCrop() {
        if (hasTransforms()) {
            throw new IllegalStateException("Can't apply centerCrop after transform has been called." +
            "centerCrop is applied to the original resized bitmap.");
        }
        if (resizeWidth <= 0 || resizeHeight <= 0)
            throw new IllegalStateException("must call resize first");
        scaleMode = ScaleMode.CenterCrop;
        return this;
    }

    @Override
    public IonBitmapRequestBuilder centerInside() {
        if (hasTransforms()) {
            throw new IllegalStateException("Can't apply centerInside after transform has been called." +
            "centerInside is applied to the original resized bitmap.");
        }
        if (resizeWidth <= 0 || resizeHeight <= 0)
            throw new IllegalStateException("must call resize first");
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

    @Override
    public IonBitmapRequestBuilder disableFadeIn() {
        this.disableFadeIn = true;
        return this;
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
        if (Build.VERSION.SDK_INT < 10)
            return this;
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
