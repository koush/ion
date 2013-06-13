package com.koushikdutta.ion;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import com.koushikdutta.ion.bitmap.BitmapInfo;

/**
 * Created by koush on 6/8/13.
 */
public class IonDrawable extends Drawable {
    private Paint paint;
    private Bitmap bitmap;
    private BitmapInfo info;
    private int placeholderResource;
    private Drawable placeholder;
    private int errorResource;
    private Drawable error;
    private Resources resources;
    int loadedFrom;

    private static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

    public IonDrawable(Resources resources) {
        this.resources = resources;
        paint = new Paint(DEFAULT_PAINT_FLAGS);
    }


    public IonDrawable setBitmap(BitmapInfo info, int loadedFrom) {
        this.loadedFrom = loadedFrom;

        if (this.info == info)
            return this;

        invalidateSelf();

        this.info = info;
        if (info == null) {
            bitmap = null;
            return this;
        }

        this.bitmap = info.bitmap;
        return this;
    }

    public IonDrawable setError(int resource, Drawable drawable) {
        if (drawable == this.error && resource == errorResource)
            return this;

        this.errorResource = resource;
        this.error = drawable;
        invalidateSelf();
        return this;
    }

    public IonDrawable setPlaceholder(int resource, Drawable drawable) {
        if (drawable == this.placeholder && resource == placeholderResource)
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

        if (bitmap == null) {
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
                invalidateSelf();
            }
        }

        paint.setAlpha((int)destAlpha);
        canvas.drawBitmap(bitmap, null, getBounds(), paint);
        paint.setAlpha(0xFF);

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

    @Override
    public Drawable mutate() {
        throw new UnsupportedOperationException();
    }
}