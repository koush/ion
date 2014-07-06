package com.koushikdutta.ion;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.util.FileCache;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.future.ImageViewFuture;

import java.lang.ref.WeakReference;

/**
 * Created by koush on 6/8/13.
 */
class IonDrawable extends Drawable {
    private Paint paint;
    private BitmapInfo info;
    private int placeholderResource;
    private Drawable placeholder;
    private int errorResource;
    private Drawable error;
    private Resources resources;
    private int loadedFrom;
    private IonDrawableCallback callback;
    private boolean disableFadeIn;
    private int resizeWidth;
    private int resizeHeight;
    private Ion ion;

    public IonDrawable ion(Ion ion) {
        this.ion = ion;
        return this;
    }

    public Drawable getCurrentDrawable() {
        if (info == null) {
            if (placeholderResource != 0)
                return resources.getDrawable(placeholderResource);
        }
        if (info != null && info.bitmaps != null)
            return new BitmapDrawable(resources, info.bitmaps[0]);
        if (errorResource != 0)
            return resources.getDrawable(errorResource);
        return null;
    }

    public BitmapInfo getBitmapInfo() {
        return info;
    }

    public static class ImageViewFutureImpl extends SimpleFuture<ImageView> implements ImageViewFuture {
        @Override
        public Future<ImageViewBitmapInfo> withBitmapInfo() {
            final SimpleFuture<ImageViewBitmapInfo> ret = new SimpleFuture<ImageViewBitmapInfo>();
            setCallback(new FutureCallback<ImageView>() {
                @Override
                public void onCompleted(Exception e, ImageView result) {
                    ImageViewBitmapInfo val = new ImageViewBitmapInfo();
                    Drawable d = null;
                    if (result != null)
                        d = result.getDrawable();
                    if (d instanceof IonDrawable) {
                        IonDrawable id = (IonDrawable)d;
                        val.info = id.info;
                    }
                    val.exception = e;
                    val.imageView = result;
                    ret.setComplete(val);
                }
            });
            ret.setParent(this);
            return ret;
        }
    }

    public ImageViewFutureImpl getFuture() {
        return callback.imageViewFuture;
    }
    
    public IonDrawable setDisableFadeIn(boolean disableFadeIn) {
        this.disableFadeIn = disableFadeIn;
        return this;
    }

    public IonDrawable setInAnimation(Animation inAnimation, int inAnimationResource) {
        callback.inAnimation = inAnimation;
        callback.inAnimationResource = inAnimationResource;
        return this;
    }

    // create an internal static class that can act as a callback.
    // dont let it hold strong references to anything.
    static class IonDrawableCallback implements FutureCallback<BitmapInfo> {
        private WeakReference<IonDrawable> ionDrawableRef;
        private ContextReference.ImageViewContextReference imageViewRef;
        private String bitmapKey;
        private ImageViewFutureImpl imageViewFuture = new ImageViewFutureImpl();
        private Animation inAnimation;
        private int inAnimationResource;

        public IonDrawableCallback(IonDrawable drawable, ImageView imageView) {
            ionDrawableRef = new WeakReference<IonDrawable>(drawable);
            imageViewRef = new ContextReference.ImageViewContextReference(imageView);
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

            imageView.setImageDrawable(null);
            drawable.setBitmap(result, result.loadedFrom);
            imageView.setImageDrawable(drawable);
            IonBitmapRequestBuilder.doAnimation(imageView, inAnimation, inAnimationResource);

            if (null != imageViewRef.isAlive()) {
                imageViewFuture.cancelSilently();
                return;
            }

            imageViewFuture.setComplete(e, imageView);
        }
    }

    public void cancel() {
        if (callback.bitmapKey == null)
            return;
        ion.bitmapsPending.removeItem(callback.bitmapKey, callback);
        callback.bitmapKey = null;
    }

    private static void unregister(Ion ion, String key, IonDrawableCallback callback) {
        if (key == null)
            return;
        // unregister this drawable from the bitmaps that are
        // pending.

        // if this drawable was the only thing waiting for this bitmap,
        // then the removeItem call will return the TransformBitmap/LoadBitmap instance
        // that was providing the result.
        if (ion.bitmapsPending.removeItem(key, callback)) {
            // find out who owns this thing, to see if it is a candidate for removal
            Object owner = ion.bitmapsPending.tag(key);
            if (owner instanceof TransformBitmap) {
                TransformBitmap info = (TransformBitmap)owner;
                ion.bitmapsPending.remove(info.key);
                // this transform is also backed by a LoadBitmap* or a DeferredLoadBitmap, grab that
                // if it is the only waiter
                if (ion.bitmapsPending.removeItem(info.downloadKey, info))
                    owner = ion.bitmapsPending.tag(info.downloadKey);
            }
            if (owner instanceof DeferredLoadBitmap) {
                DeferredLoadBitmap defer = (DeferredLoadBitmap)owner;
                ion.bitmapsPending.remove(defer.key);
            }
        }

        ion.processDeferred();
    }

    public void register(Ion ion, String bitmapKey) {
        String previousKey = callback.bitmapKey;
        if (TextUtils.equals(previousKey, bitmapKey))
            return;
        callback.bitmapKey = bitmapKey;
        ion.bitmapsPending.add(bitmapKey, callback);
        unregister(ion, previousKey, callback);
    }

    private static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

    public IonDrawable(Resources resources, ImageView imageView) {
        this.resources = resources;
        paint = new Paint(DEFAULT_PAINT_FLAGS);
        callback = new IonDrawableCallback(this, imageView);
    }

    int currentFrame;
    private boolean invalidateScheduled;
    private int textureDim;
    private int maxLevel;
    public IonDrawable setBitmap(BitmapInfo info, int loadedFrom) {
        this.loadedFrom = loadedFrom;

        if (this.info == info)
            return this;

        invalidateSelf();

        this.info = info;
        currentFrame = 0;
        invalidateScheduled = false;
        if (info == null) {
            callback.bitmapKey = null;
            return this;
        }

        if (info.decoder != null) {
            // find number of tiles across to fit
            double wlevel = (double)info.originalSize.x / TILE_DIM;
            double hlevel = (double)info.originalSize.y / TILE_DIM;

            // find the level: find how many power of 2 tiles are necessary
            // to fit the entire image. ie, fit it into a square.
            double level = Math.max(wlevel, hlevel);
            level = Math.log(level) / LOG_2;

            maxLevel = (int)Math.ceil(level);

            // now, we know the entire image will fit in a square image of
            // this dimension:
            textureDim = TILE_DIM << maxLevel;
        }

        callback.bitmapKey = info.key;
        return this;
    }

    public IonDrawable setSize(int resizeWidth, int resizeHeight) {
        if (this.resizeWidth == resizeWidth && this.resizeHeight == resizeHeight)
            return this;
        this.resizeWidth = resizeWidth;
        this.resizeHeight = resizeHeight;
        invalidateSelf();
        return this;
    }

    public IonDrawable setError(int resource, Drawable drawable) {
        if ((drawable != null && drawable == error) || (resource != 0 && resource == errorResource))
            return this;

        errorResource = resource;
        if (error != null)
            error.setCallback(null);
        error = drawable;
        invalidateSelf();
        return this;
    }

    public IonDrawable setPlaceholder(int resource, Drawable drawable) {
        if ((drawable != null && drawable == placeholder) || (resource != 0 && resource == placeholderResource))
            return this;

        placeholderResource = resource;
        if (placeholder != null)
            placeholder.setCallback(null);
        placeholder = drawable;
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

    Callback drawableCallback = new Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            IonDrawable.this.invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            IonDrawable.this.scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            IonDrawable.this.unscheduleSelf(what);
        }
    };

    private Drawable tryGetErrorResource() {
        if (error != null)
            return error;
        if (errorResource == 0)
            return null;
        error = resources.getDrawable(errorResource);
        error.setCallback(drawableCallback);
        return error;
    }

    private Drawable tryGetPlaceholderResource() {
        if (placeholder != null)
            return placeholder;
        if (placeholderResource == 0)
            return null;
        placeholder = resources.getDrawable(placeholderResource);
        placeholder.setCallback(drawableCallback);
        return placeholder;
    }

    @Override
    public int getIntrinsicWidth() {
        // first check if image was loaded
        if (info != null) {
            if (info.decoder != null)
                return info.originalSize.x;
            if (info.bitmaps != null)
                return info.bitmaps[0].getScaledWidth(resources.getDisplayMetrics().densityDpi);
        }
        // check eventual image size...
        if (resizeWidth > 0)
            return resizeWidth;
        // no image, but there was an error
        if (info != null) {
            Drawable error = tryGetErrorResource();
            if (error != null)
                return error.getIntrinsicWidth();
        }
        // check placeholder
        Drawable placeholder = tryGetPlaceholderResource();
        if (placeholder != null)
            return placeholder.getIntrinsicWidth();
        // we're SOL
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        if (info != null) {
            if (info.decoder != null)
                return info.originalSize.y;
            if (info.bitmaps != null)
                return info.bitmaps[0].getScaledHeight(resources.getDisplayMetrics().densityDpi);
        }
        if (resizeHeight > 0)
            return resizeHeight;
        if (info != null) {
            Drawable error = tryGetErrorResource();
            if (error != null)
                return error.getIntrinsicHeight();
        }
        Drawable placeholder = tryGetPlaceholderResource();
        if (placeholder != null)
            return placeholder.getIntrinsicHeight();
        return -1;
    }

    public static final long FADE_DURATION = 200;
    private Runnable invalidate = new Runnable() {
        @Override
        public void run() {
            invalidateScheduled = false;
            currentFrame++;
            invalidateSelf();
        }
    };

    private static final double LOG_2 = Math.log(2);
    private static final int TILE_DIM = 256;

    FutureCallback<BitmapInfo> tileCallback = new FutureCallback<BitmapInfo>() {
        @Override
        public void onCompleted(Exception e, BitmapInfo result) {
            invalidateSelf();
        }
    };

    private void drawDrawable(Canvas canvas, Drawable d) {
        if (d == null)
            return;

        if (false) {
            // this centers inside and draws the drawable
            d.setBounds(0 , 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            int count = canvas.save();
            Matrix matrix = new Matrix();
            matrix.setRectToRect(new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight()),
            new RectF(canvas.getClipBounds()), Matrix.ScaleToFit.CENTER);
            canvas.concat(matrix);

            float scale = (float)canvas.getClipBounds().width() / canvas.getWidth();
            canvas.scale(scale, scale, d.getIntrinsicWidth() / 2, d.getIntrinsicHeight() / 2);

            d.draw(canvas);
            canvas.restoreToCount(count);
            return;
        }
        else if (false) {
            // this centers fits and draws the drawable
            int iw = d.getIntrinsicWidth();
            int ih = d.getIntrinsicHeight();

            Rect b = copyBounds();
            int w = b.width();
            int h = b.height();
            if (iw >= 0) {
                int wp = (w - iw) / 2;
                b.left += wp;
                b.right = b.left + iw;
            }
            if (ih >= 0) {
                int hp = (h - ih) / 2;
                b.top += hp;
                b.bottom = b.top + ih;
            }
            d.setBounds(b);
        }
        else {
            // fitxy the drwable
            d.setBounds(getBounds());
        }

        d.draw(canvas);
    }

    @Override
    public void draw(Canvas canvas) {
        // TODO: handle animated drawables
        if (info == null) {
            drawDrawable(canvas, tryGetPlaceholderResource());
            return;
        }

        if (info.drawTime == 0)
            info.drawTime = SystemClock.uptimeMillis();

        long destAlpha = 0xFF;

        if(!disableFadeIn) {
            destAlpha = ((SystemClock.uptimeMillis() - info.drawTime) << 8) / FADE_DURATION;
            destAlpha = Math.min(destAlpha, 0xFF);
        }

        if (destAlpha != 255) {
            Drawable placeholder = tryGetPlaceholderResource();
            if (placeholder != null) {
                drawDrawable(canvas, placeholder);
            }
        }

        if (info.decoder != null) {
            // zoom 0: entire image fits in a TILE_DIMxTILE_DIM square

            // draw base bitmap for empty tiles
            // figure out zoom level
            // figure out which tiles need rendering
            // draw stuff that needs drawing
            // missing tile? fetch it
            // use parent level tiles for tiles that do not exist

            // TODO: crossfading?

            Rect clip = canvas.getClipBounds();
            Rect bounds = getBounds();

            float zoom = (float)canvas.getWidth() / (float)clip.width();

            float zoomWidth = zoom * bounds.width();
            float zoomHeight = zoom * bounds.height();

            double wlevel = Math.log(zoomWidth / TILE_DIM) / LOG_2;
            double hlevel = Math.log(zoomHeight/ TILE_DIM) / LOG_2;
            double maxLevel = Math.max(wlevel, hlevel);

            int visibleLeft = Math.max(0, clip.left);
            int visibleRight = Math.min(bounds.width(), clip.right);
            int visibleTop = Math.max(0, clip.top);
            int visibleBottom = Math.min(bounds.height(), clip.bottom);
            int level = (int)Math.floor(maxLevel);
            level = Math.min(this.maxLevel, level);
            level = Math.max(level, 0);
            int levelTiles = 1 << level;
            int textureTileDim = textureDim / levelTiles;
//            System.out.println("textureTileDim: " + textureTileDim);

//            System.out.println(info.key + " visible: " + new Rect(visibleLeft, visibleTop, visibleRight, visibleBottom));

            final boolean DEBUG_ZOOM = false;
            if (info.bitmaps != null && info.bitmaps[0] != null) {
                canvas.drawBitmap(info.bitmaps[0], null, getBounds(), paint);
                if (DEBUG_ZOOM) {
                    paint.setColor(Color.RED);
                    paint.setAlpha(0x80);
                    canvas.drawRect(getBounds(), paint);
                    paint.setAlpha(0xFF);
                }
            }
            else {
                paint.setColor(Color.BLACK);
                canvas.drawRect(getBounds(), paint);
            }

            int sampleSize = 1;
            while (textureTileDim / sampleSize > TILE_DIM)
                sampleSize <<= 1;

            for (int y = 0; y < levelTiles; y++) {
                int top = textureTileDim * y;
                int bottom = textureTileDim * (y + 1);
                bottom = Math.min(bottom, bounds.bottom);
                // TODO: start at visible pos
                if (bottom < visibleTop)
                    continue;
                if (top > visibleBottom)
                    break;
                for (int x = 0; x < levelTiles; x++) {
                    int left = textureTileDim * x;
                    int right = textureTileDim * (x + 1);
                    right = Math.min(right, bounds.right);
                    // TODO: start at visible pos
                    if (right < visibleLeft)
                        continue;
                    if (left > visibleRight)
                        break;

                    Rect texRect = new Rect(left, top, right, bottom);

                    // find, render/fetch
//                    System.out.println("rendering: " + texRect + " for: " + bounds);
                    String tileKey = FileCache.toKeyString(info.key, ",", level, ",", x, ",", y);
                    BitmapInfo tile = ion.bitmapCache.get(tileKey);
                    if (tile != null && tile.bitmaps != null) {
                        // render it
//                        System.out.println("bitmap is: " + tile.bitmaps[0].getWidth() + "x" + tile.bitmaps[0].getHeight());
                        canvas.drawBitmap(tile.bitmaps[0], null, texRect, paint);
                        continue;
                    }

                    // TODO: cancellation of unnecessary regions when fast pan/zooming
                    if (ion.bitmapsPending.tag(tileKey) == null) {
                        // fetch it
//                        System.out.println(info.key + ": fetching region: " + texRect + " sample size: " + sampleSize);
                        LoadBitmapRegion region = new LoadBitmapRegion(ion, tileKey, info.decoder, texRect, sampleSize);
                    }
                    ion.bitmapsPending.add(tileKey, tileCallback);

                    int parentLeft = 0;
                    int parentTop = 0;
                    int parentUp = 1;
                    int parentLevel = level - parentUp;
                    if (x % 2 == 1)
                        parentLeft++;
                    if (y % 2 == 1)
                        parentTop++;
                    int parentX = x >> 1;
                    int parentY = y >> 1;

                    while (parentLevel >= 0) {
                        tileKey = FileCache.toKeyString(info.key, ",", parentLevel, ",", parentX, ",", parentY);
                        tile = ion.bitmapCache.get(tileKey);
                        if (tile != null && tile.bitmaps != null)
                            break;
                        if (parentX % 2 == 1) {
                            parentLeft += 1 << parentUp;
                        }
                        if (parentY % 2 == 1) {
                            parentTop += 1 << parentUp;
                        }
                        parentLevel--;
                        parentUp++;
                        parentX >>= 1;
                        parentY >>= 1;
                    }

                    // well, i give up
                    if (tile == null || tile.bitmaps == null)
                        continue;


                    int subLevelTiles = 1 << parentLevel;
                    int subtileDim = textureDim / subLevelTiles;
                    int subSampleSize = 1;
                    while (subtileDim / subSampleSize > TILE_DIM)
                        subSampleSize <<= 1;
                    int subTextureDim = subtileDim / subSampleSize;
//                    System.out.println(String.format("falling back for %s,%s,%s to %s,%s,%s: %s,%s (%s to %s)", x, y, level, parentX, parentY, parentLevel, parentLeft, parentTop, subTextureDim, subTextureDim >> parentUp));
                    subTextureDim >>= parentUp;
                    int sourceLeft = subTextureDim * parentLeft;
                    int sourceTop = subTextureDim * parentTop;
                    Rect sourceRect = new Rect(sourceLeft, sourceTop, sourceLeft + subTextureDim, sourceTop + subTextureDim);
                    canvas.drawBitmap(tile.bitmaps[0], sourceRect, texRect, paint);

                    if (DEBUG_ZOOM) {
                        paint.setColor(Color.RED);
                        paint.setAlpha(0x80);
                        canvas.drawRect(texRect, paint);
                        paint.setAlpha(0xFF);
                    }
                }
            }
        }
        else if (info.bitmaps != null) {
            paint.setAlpha((int)destAlpha);
            canvas.drawBitmap(info.bitmaps[currentFrame % info.bitmaps.length], null, getBounds(), paint);
            paint.setAlpha(0xFF);
            if (info.delays != null) {
                int delay = info.delays[currentFrame % info.delays.length];
                if (!invalidateScheduled) {
                    invalidateScheduled = true;
                    unscheduleSelf(invalidate);
                    scheduleSelf(invalidate, SystemClock.uptimeMillis() + Math.max(delay, 100));
                }
            }
        }
        else {
            Drawable error = tryGetErrorResource();
            if (error != null) {
                error.setAlpha((int)destAlpha);
                drawDrawable(canvas, error);
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
        return (info == null || info.bitmaps == null || info.bitmaps[0].hasAlpha() || paint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    static IonDrawable getOrCreateIonDrawable(ImageView imageView) {
        Drawable current = imageView.getDrawable();
        IonDrawable ret;
        if (current == null || !(current instanceof IonDrawable))
            ret = new IonDrawable(imageView.getResources(), imageView);
        else
            ret = (IonDrawable)current;
        // invalidate self doesn't seem to trigger the dimension check to be called by imageview.
        // are drawable dimensions supposed to be immutable?
        imageView.setImageDrawable(null);
        return ret;
    }
}