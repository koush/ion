package com.koushikdutta.ion;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by koush on 6/8/13.
 */
class IonDrawable extends Drawable {
    private Paint paint;
    private Bitmap bitmap;
    private BitmapInfo info;
    private int placeholderResource;
    private Drawable placeholder;
    private int errorResource;
    private Drawable error;
    private Resources resources;
    private int loadedFrom;
    private IonDrawableCallback callback;

    public IonDrawable cancel() {
        requestCount++;
        return this;
    }

    public SimpleFuture<ImageView> getFuture() {
        return callback.imageViewFuture;
    }

    public void setInAnimation(Animation inAnimation, int inAnimationResource) {
        callback.inAnimation = inAnimation;
        callback.inAnimationResource = inAnimationResource;
    }

    // create an internal static class that can act as a callback.
    // dont let it hold strong references to anything.
    static class IonDrawableCallback implements FutureCallback<BitmapInfo> {
        private WeakReference<IonDrawable> ionDrawableRef;
        private WeakReference<ImageView> imageViewRef;
        private String bitmapKey;
        private SimpleFuture<ImageView> imageViewFuture = new SimpleFuture<ImageView>();
        private Animation inAnimation;
        private int inAnimationResource;
        private int requestId;

        public IonDrawableCallback(IonDrawable drawable, ImageView imageView) {
            ionDrawableRef = new WeakReference<IonDrawable>(drawable);
            imageViewRef = new WeakReference<ImageView>(imageView);
        }

        @Override
        public void onCompleted(Exception e, BitmapInfo result) {
            assert Thread.currentThread() == Looper.getMainLooper().getThread();
            assert result != null;

            // see if the imageview is still alive and cares about this result
            ImageView imageView = imageViewRef.get();
            if (imageView == null)
                return;

            IonDrawable drawable = ionDrawableRef.get();
            if (drawable == null)
                return;

            if (imageView.getDrawable() != drawable)
                return;

            // see if the ImageView is still waiting for the same request
            if (drawable.requestCount != requestId)
                return;

            drawable.requestCount++;
            imageView.setImageDrawable(null);
            drawable.setBitmap(result, result.loadedFrom);
            imageView.setImageDrawable(drawable);
            IonBitmapRequestBuilder.doAnimation(imageView, inAnimation, inAnimationResource);
            imageViewFuture.setComplete(imageView);

        }
    }

    int requestCount;
    public void register(Ion ion, String bitmapKey) {
        callback.requestId = ++requestCount;
        String previousKey = callback.bitmapKey;
        if (TextUtils.equals(previousKey, bitmapKey))
            return;
        callback.bitmapKey = bitmapKey;
        ion.bitmapsPending.add(bitmapKey, callback);
        if (previousKey == null)
            return;

        ArrayList<FutureCallback<BitmapInfo>> cbs = ion.bitmapsPending.get(previousKey);
        if (cbs == null)
            return;

        cbs.remove(callback);
        if (cbs.size() == 0)
            ion.bitmapsPending.remove(previousKey);
    }

    private static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

    public IonDrawable(Resources resources, ImageView imageView) {
        this.resources = resources;
        paint = new Paint(DEFAULT_PAINT_FLAGS);
        callback = new IonDrawableCallback(this, imageView);
    }


    public IonDrawable setBitmap(BitmapInfo info, int loadedFrom) {
        this.loadedFrom = loadedFrom;

        if (this.info == info)
            return this;

        invalidateSelf();

        this.info = info;
        if (info == null) {
            callback.bitmapKey = null;
            bitmap = null;
            return this;
        }

        callback.bitmapKey = info.key;
        this.bitmap = info.bitmap;
        return this;
    }

    public IonDrawable setError(int resource, Drawable drawable) {
        if ((drawable != null && drawable == this.error) || (resource != 0 && resource == errorResource))
            return this;

        this.errorResource = resource;
        this.error = drawable;
        invalidateSelf();
        return this;
    }

    public IonDrawable setPlaceholder(int resource, Drawable drawable) {
        if ((drawable != null && drawable == this.placeholder) || (resource != 0 && resource == placeholderResource))
            return this;

        this.placeholderResource = resource;
        this.placeholder = drawable;
        invalidateSelf();

        return this;
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        paint.setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
        paint.setDither(dither);
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        if (bitmap != null)
            return bitmap.getScaledWidth(resources.getDisplayMetrics().densityDpi);
        if (error != null)
            return error.getIntrinsicWidth();
        if (placeholder != null)
            return placeholder.getIntrinsicWidth();
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        if (bitmap != null)
            return bitmap.getScaledHeight(resources.getDisplayMetrics().densityDpi);
        if (error != null)
            return error.getIntrinsicHeight();
        if (placeholder != null)
            return placeholder.getIntrinsicHeight();
        return -1;
    }

    public static final long FADE_DURATION = 200;

    @Override
    public void draw(Canvas canvas) {
        if (info == null) {
            if (placeholder == null && placeholderResource != 0)
                placeholder = resources.getDrawable(placeholderResource);
            if (placeholder != null) {
                placeholder.setBounds(getBounds());
                placeholder.draw(canvas);
            }
            return;
        }

        if (info.drawTime == 0)
            info.drawTime = SystemClock.uptimeMillis();
        long destAlpha = ((SystemClock.uptimeMillis() - info.drawTime) << 8) / FADE_DURATION;
        destAlpha = Math.min(destAlpha, 0xFF);

        if (destAlpha != 255) {
            if (placeholder == null && placeholderResource != 0)
                placeholder = resources.getDrawable(placeholderResource);
            if (placeholder != null) {
                placeholder.setBounds(getBounds());
                placeholder.draw(canvas);
            }
        }

        if (bitmap != null) {
            paint.setAlpha((int)destAlpha);
            canvas.drawBitmap(bitmap, null, getBounds(), paint);
            paint.setAlpha(0xFF);
        }
        else {
            if (error == null && errorResource != 0)
                error = resources.getDrawable(errorResource);
            if (error != null) {
                error.setAlpha((int)destAlpha);
                error.setBounds(getBounds());
                error.draw(canvas);
                error.setAlpha(0xFF);
            }
        }

        if (destAlpha != 255)
            invalidateSelf();

        if (true)
            return;

        // stolen from picasso
        canvas.save();
        canvas.rotate(45);

        paint.setColor(Color.WHITE);
        canvas.drawRect(0, -10, 7.5f, 10, paint);

        int sourceColor;
        switch (loadedFrom) {
            case Loader.LoaderEmitter.LOADED_FROM_CACHE:
                sourceColor = Color.CYAN;
                break;
            case Loader.LoaderEmitter.LOADED_FROM_CONDITIONAL_CACHE:
                sourceColor = Color.YELLOW;
                break;
            case Loader.LoaderEmitter.LOADED_FROM_MEMORY:
                sourceColor = Color.GREEN;
                break;
            default:
                sourceColor = Color.RED;
                break;
        }

        paint.setColor(sourceColor);
        canvas.drawRect(0, -9, 6.5f, 9, paint);

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
       paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return (bitmap == null || bitmap.hasAlpha() || paint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    static IonDrawable getOrCreateIonDrawable(ImageView imageView) {
        Drawable current = imageView.getDrawable();
        IonDrawable ret;
        if (current == null || !(current instanceof IonDrawable)) {
            ret = new IonDrawable(imageView.getResources(), imageView);
            imageView.setImageDrawable(ret);
        }
        else {
            ret = (IonDrawable)current;
        }
        // invalidate self doesn't seem to trigger the dimension check to be called by imageview.
        // are drawable dimensions supposed to be immutable?
        imageView.setImageDrawable(null);
        return ret;
    }
}