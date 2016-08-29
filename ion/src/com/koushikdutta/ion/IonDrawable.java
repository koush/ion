package com.koushikdutta.ion;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.ImageView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.util.FileCache;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.gif.GifDecoder;
import com.koushikdutta.ion.gif.GifFrame;

import java.lang.ref.WeakReference;

/**
 * Created by koush on 6/8/13.
 */
class IonDrawable extends LayerDrawable {
    private static final double LOG_2 = Math.log(2);
    private static final int TILE_DIM = 256;
    private static final long FADE_DURATION = 200;
    private static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

    private Paint paint;
    private int alpha = 0xFF;
    private BitmapInfo info;
    private int placeholderResource;
    private Drawable placeholder;
    private int errorResource;
    private Drawable error;
    private Resources resources;
    private ResponseServedFrom servedFrom;
    private boolean fadeIn;
    private int resizeWidth;
    private int resizeHeight;
    private boolean repeatAnimation;
    private Ion ion;
    private BitmapFetcher bitmapFetcher;
    private IonDrawableCallback callback;
    private FutureCallback<IonDrawable> loadCallback;
    private IonGifDecoder gifDecoder;
    private Drawable bitmapDrawable;
    private int textureDim;
    private int maxLevel;
    private BitmapDrawableFactory bitmapDrawableFactory;

    private final Drawable NULL_PLACEHOLDER;
    private final Drawable NULL_BITMAPINFO;
    private final Drawable NULL_ERROR;

    public FutureCallback<IonDrawable> getLoadCallback() {
        return loadCallback;
    }

    public IonDrawable setLoadCallback(FutureCallback<IonDrawable> loadCallback) {
        this.loadCallback = loadCallback;
        return this;
    }

    public IonDrawable ion(Ion ion) {
        if (ion == null)
            throw new AssertionError("null ion");
        this.ion = ion;
        return this;
    }

    public Drawable getCurrentDrawable() {
        if (info == null) {
            if (placeholderResource != 0)
                return resources.getDrawable(placeholderResource);
        }
        if (info != null) {
            if (info.bitmap != null)
                return new BitmapDrawable(resources, info.bitmap);
            else if (info.gifDecoder != null) {
                GifFrame last = info.gifDecoder.getLastFrame();
                if (last != null)
                    return new BitmapDrawable(resources, last.image);
                if (placeholderResource != 0)
                    return resources.getDrawable(placeholderResource);
                return null;
            }
        }
        if (errorResource != 0)
            return resources.getDrawable(errorResource);
        return null;
    }

    public BitmapInfo getBitmapInfo() {
        return info;
    }

    // create an internal static class that can act as a callback.
    // dont let it hold strong references to anything.
    static class IonDrawableCallback implements FutureCallback<BitmapInfo> {
        private WeakReference<IonDrawable> ionDrawableRef;
        private String bitmapKey;
        private Ion ion;
        public IonDrawableCallback(IonDrawable drawable) {
            ionDrawableRef = new WeakReference<IonDrawable>(drawable);
//            imageViewRef = new ContextReference.ImageViewContextReference(imageView);
        }

        public void register(Ion ion, String bitmapKey) {
            String previousKey = this.bitmapKey;
            Ion previousIon = this.ion;
            if (TextUtils.equals(previousKey, bitmapKey) && this.ion == ion)
                return;
            this.ion = ion;
            this.bitmapKey = bitmapKey;
            if (ion != null)
                ion.bitmapsPending.add(bitmapKey, this);
            unregister(previousIon, previousKey);
        }

        private void unregister(Ion ion, String key) {
            if (key == null)
                return;
            // unregister this drawable from the bitmaps that are
            // pending.

            // if this drawable was the only thing waiting for this bitmap,
            // then the removeItem call will return the TransformBitmap/LoadBitmap instance
            // that was providing the result.
            if (ion.bitmapsPending.removeItem(key, this)) {
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
                // only cancel deferred loads... LoadBitmap means a download is already in progress.
                // due to view recycling, cancelling that may be bad, as it may be rerequested again
                // during the recycle process.
                if (owner instanceof DeferredLoadBitmap) {
                    DeferredLoadBitmap defer = (DeferredLoadBitmap)owner;
                    ion.bitmapsPending.remove(defer.key);
                }
            }

            ion.processDeferred();
        }

        @Override
        public void onCompleted(Exception e, BitmapInfo result) {
            assert Thread.currentThread() == Looper.getMainLooper().getThread();
            assert result != null;
            // see if the imageview is still alive and cares about this result
            IonDrawable drawable = ionDrawableRef.get();
            if (drawable == null)
                return;
            drawable
            .setBitmap(result, result.servedFrom)
            .updateLayers();
            FutureCallback<IonDrawable> callback = drawable.loadCallback;
            if (callback != null)
                callback.onCompleted(e, drawable);
        }
    }

    class IonGifDecoder {
        GifDecoder gifDecoder;
        Exception exception;
        GifFrame currentFrame;
        long nextFrameRender;
        public IonGifDecoder(BitmapInfo info){
            gifDecoder = info.gifDecoder.mutate();
            currentFrame = gifDecoder.getLastFrame();
        }

        Runnable loader = new Runnable() {
            @Override
            public void run() {
                try {
                    gifDecoder.nextFrame();
                }
                catch (OutOfMemoryError e) {
                    exception = new Exception(e);
                }
                catch (Exception e) {
                    exception = e;
                }
                Ion.mainHandler.post(postLoad);
            }
        };

        Runnable postLoad = new Runnable() {
            @Override
            public void run() {
                isLoading = false;
                invalidateSelf();
            }
        };

        long getDelay() {
            // error case?
            if (currentFrame == null)
                return 1000 / 10;
            long delay = currentFrame.delay;
            if (delay == 0)
                delay = 1000 / 10;
            return delay;
        }

        public GifFrame getCurrentFrame() {
            long now = System.currentTimeMillis();
            if (nextFrameRender == 0) {
                nextFrameRender = now + getDelay();
                scheduleNextFrame();
            }

            if (now >= nextFrameRender) {
                // see if a frame is available
                if (gifDecoder.getLastFrame() != currentFrame) {
                    // we have a frame waiting, grab it i guess.
                    currentFrame = gifDecoder.getLastFrame();
                    // check if we need to drop frames, or maintain timing
                    if (now > nextFrameRender + getDelay())
                        nextFrameRender = now + getDelay();
                    else
                        nextFrameRender += getDelay();
                }
                scheduleNextFrame();
            }

            return currentFrame;
        }

        boolean isLoading;
        public synchronized void scheduleNextFrame() {
            if (isLoading)
                return;
            if (exception != null)
                return;
            if (gifDecoder.getStatus() == GifDecoder.STATUS_FINISH && repeatAnimation)
                gifDecoder.restart();
            isLoading = true;
            Ion.getBitmapLoadExecutorService().execute(loader);
        }
    }

    public IonDrawable setFadeIn(boolean fadeIn) {
        this.fadeIn = fadeIn;
        return this;
    }

    public IonDrawable setBitmapFetcher(BitmapFetcher bitmapFetcher) {
        this.bitmapFetcher = bitmapFetcher;
        if (ion == null)
            throw new AssertionError("null ion");
        return this;
    }

    public IonDrawable setBitmapDrawableFactory(BitmapDrawableFactory factory) {
        this.bitmapDrawableFactory = factory;
        return this;
    }

    public void cancel() {
        callback.register(null, null);
        bitmapFetcher = null;
    }

    public IonDrawable(Resources resources) {
        super(new Drawable[] { new BitmapDrawable((Bitmap)null), new BitmapDrawable((Bitmap)null), new BitmapDrawable((Bitmap)null) });

        setId(0, 0);
        setId(1, 1);
        setId(2, 2);

        NULL_PLACEHOLDER = getDrawable(0);
        NULL_BITMAPINFO = getDrawable(1);
        NULL_ERROR = getDrawable(2);

        this.resources = resources;
        paint = new Paint(DEFAULT_PAINT_FLAGS);
        callback = new IonDrawableCallback(this);
    }

    public IonDrawable updateLayers() {
        // always set up the placeholder, it will disappear automagically
        tryGetPlaceholderResource();
        if (placeholder == null)
            setDrawableByLayerId(0, NULL_PLACEHOLDER);
        else
            setDrawableByLayerId(0, placeholder);

        if (info == null) {
            setDrawableByLayerId(1, NULL_BITMAPINFO);
            setDrawableByLayerId(2, NULL_ERROR);
            return this;
        }

        // error case
        if (info.bitmap == null && info.decoder == null && info.gifDecoder == null) {
            setDrawableByLayerId(1, NULL_BITMAPINFO);
            tryGetErrorResource();
            if (error == null)
                setDrawableByLayerId(2, NULL_ERROR);
            else
                setDrawableByLayerId(2, error);
            return this;
        }

        if (info.decoder == null && info.gifDecoder == null) {
            // normal bitmap
            tryGetBitmapResource();
            setDrawableByLayerId(1, bitmapDrawable);
        }
        else {
            // gif or deepzoom
            setDrawableByLayerId(1, NULL_BITMAPINFO);
        }
        setDrawableByLayerId(2, NULL_ERROR);
        return this;
    }

    public IonDrawable setBitmap(BitmapInfo info, ResponseServedFrom servedFrom) {
        if (this.info == info)
            return this;

        cancel();
        this.servedFrom = servedFrom;
        this.info = info;
        gifDecoder = null;
        bitmapDrawable = null;
        invalidateSelf();
        if (info == null)
            return this;

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
        else if (info.gifDecoder != null) {
            gifDecoder = new IonGifDecoder(info);
        }

        return this;
    }

    public IonDrawable setRepeatAnimation(boolean repeatAnimation) {
        this.repeatAnimation = repeatAnimation;
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
        error = drawable;
        return this;
    }

    public IonDrawable setPlaceholder(int resource, Drawable drawable) {
        if ((drawable != null && drawable == placeholder) || (resource != 0 && resource == placeholderResource))
            return this;

        placeholderResource = resource;
        placeholder = drawable;
        return this;
    }

    private Drawable tryGetErrorResource() {
        if (error != null)
            return error;
        if (errorResource == 0)
            return null;
        error = resources.getDrawable(errorResource);
        return error;
    }

    private Drawable tryGetBitmapResource() {
        if (bitmapDrawable != null)
            return bitmapDrawable;
        if (info == null)
            return null;
        if (info.gifDecoder != null)
            return null;
        if (info.decoder != null)
            return null;
        if (info.bitmap == null)
            return null;
        bitmapDrawable = bitmapDrawableFactory.fromBitmap(resources, info.bitmap);
        return bitmapDrawable;
    }

    private Drawable tryGetPlaceholderResource() {
        if (placeholder != null)
            return placeholder;
        if (placeholderResource == 0)
            return null;
        placeholder = resources.getDrawable(placeholderResource);
        return placeholder;
    }

    private FutureCallback<BitmapInfo> tileCallback = new FutureCallback<BitmapInfo>() {
        @Override
        public void onCompleted(Exception e, BitmapInfo result) {
            invalidateSelf();
        }
    };

    @Override
    public int getIntrinsicWidth() {
        // first check if image was loaded
        if (info != null) {
            if (info.decoder != null)
                return info.originalSize.x;
            if (info.bitmap != null)
                return info.bitmap.getScaledWidth(resources.getDisplayMetrics().densityDpi);
        }
        if (gifDecoder != null)
            return gifDecoder.gifDecoder.getWidth();
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
            if (info.bitmap != null)
                return info.bitmap.getScaledHeight(resources.getDisplayMetrics().densityDpi);
        }
        if (gifDecoder != null)
            return gifDecoder.gifDecoder.getHeight();
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

    @Override
    public void draw(Canvas canvas) {
        if (info == null) {
            // draw stuff
            super.draw(canvas);

            // see if we can fetch a bitmap
            if (bitmapFetcher != null) {
                if (bitmapFetcher.sampleWidth == 0 && bitmapFetcher.sampleHeight == 0) {
                    if (canvas.getWidth() != 1)
                        bitmapFetcher.sampleWidth = canvas.getWidth();
                    if (canvas.getHeight() != 1)
                        bitmapFetcher.sampleHeight = canvas.getHeight();

                    // now that we have final dimensions, reattempt to find the image in the cache
                    bitmapFetcher.recomputeDecodeKey();
                    BitmapInfo found = ion.bitmapCache.get(bitmapFetcher.bitmapKey);
                    if (found != null) {
                        // won't be needing THIS anymore
                        bitmapFetcher = null;
                        // found what we're looking for, but can't draw at this very moment,
                        // since we need to trigger a new measure.
                        callback.onCompleted(null, found);
                        return;
                    }
                }

                // no image found fetch it.
                callback.register(ion, bitmapFetcher.bitmapKey);

                // check to see if there's too many imageview loads
                // already in progress
                if (BitmapFetcher.shouldDeferImageView(ion)) {
                    bitmapFetcher.defer();
                } else {
                    bitmapFetcher.execute();
                }
                // won't be needing THIS anymore
                bitmapFetcher = null;
            }

            // well, can't do anything else here.
            return;
        }

        if (info.decoder != null) {
            drawDeepZoom(canvas);
            return;
        }

        if (info.drawTime == 0)
            info.drawTime = SystemClock.uptimeMillis();

        long destAlpha = this.alpha;

        if (fadeIn) {
            destAlpha = ((SystemClock.uptimeMillis() - info.drawTime) << 8) / FADE_DURATION;
            destAlpha = Math.min(destAlpha, this.alpha);
        }

        // remove plaeholder if not visible
        if (destAlpha == this.alpha) {
            if (placeholder != null) {
                placeholder = null;
                setDrawableByLayerId(0, NULL_PLACEHOLDER);
            }
        } else {
            // invalidate to fade in
            if (placeholder != null)
                invalidateSelf();
        }

        if (info.gifDecoder != null) {
            super.draw(canvas);

            GifFrame frame = gifDecoder.getCurrentFrame();
            if (frame != null) {
                paint.setAlpha((int) destAlpha);
                canvas.drawBitmap(frame.image, null, getBounds(), paint);
                paint.setAlpha(this.alpha);
                invalidateSelf();
            }
            return;
        }

        if (info.bitmap != null) {
            if (bitmapDrawable != null)
                bitmapDrawable.setAlpha((int)destAlpha);
        } else {
            if (error != null)
                error.setAlpha((int)destAlpha);
        }

        super.draw(canvas);

        if (true)
            return;

        // stolen from picasso
        canvas.save();
        canvas.rotate(45);

        paint.setColor(Color.WHITE);
        canvas.drawRect(0, -10, 7.5f, 10, paint);

        int sourceColor;
        if (servedFrom == ResponseServedFrom.LOADED_FROM_CACHE)
            sourceColor = Color.CYAN;
        else if (servedFrom == ResponseServedFrom.LOADED_FROM_CONDITIONAL_CACHE)
            sourceColor = Color.YELLOW;
        else if (servedFrom == ResponseServedFrom.LOADED_FROM_MEMORY)
            sourceColor = Color.GREEN;
        else
            sourceColor = Color.RED;

        paint.setColor(sourceColor);
        canvas.drawRect(0, -9, 6.5f, 9, paint);

        canvas.restore();
    }

    private void drawDeepZoom(Canvas canvas) {
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
        if (info.bitmap != null) {
            canvas.drawBitmap(info.bitmap, null, getBounds(), paint);
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
                if (tile != null && tile.bitmap != null) {
                    // render it
//                        System.out.println("bitmap is: " + tile.bitmaps[0].getWidth() + "x" + tile.bitmaps[0].getHeight());
                    canvas.drawBitmap(tile.bitmap, null, texRect, paint);
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
                    if (tile != null && tile.bitmap != null)
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
                if (tile == null || tile.bitmap == null)
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
                canvas.drawBitmap(tile.bitmap, sourceRect, texRect, paint);

                if (DEBUG_ZOOM) {
                    paint.setColor(Color.RED);
                    paint.setAlpha(0x80);
                    canvas.drawRect(texRect, paint);
                    paint.setAlpha(0xFF);
                }
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        this.alpha = alpha;
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        super.setColorFilter(cf);
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return (info == null || info.bitmap == null || info.bitmap.hasAlpha() || paint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : super.getOpacity();
    }

    static IonDrawable getOrCreateIonDrawable(ImageView imageView) {
        Drawable current = imageView.getDrawable();
        IonDrawable ret;
        if (current == null || !(current instanceof IonDrawable))
            ret = new IonDrawable(imageView.getResources());
        else
            ret = (IonDrawable)current;
        // invalidate self doesn't seem to trigger the dimension check to be called by imageview.
        // are drawable dimensions supposed to be immutable?
        imageView.setImageDrawable(null);
        return ret;
    }
}