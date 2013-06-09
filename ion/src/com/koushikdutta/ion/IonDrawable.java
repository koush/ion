package com.koushikdutta.ion;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

/**
 * Created by koush on 6/8/13.
 */
public class IonDrawable extends Drawable {
    Paint paint;
    Bitmap bitmap;
    int density;

    private static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

    public IonDrawable(Resources resources, Bitmap bitmap) {
        paint = new Paint(DEFAULT_PAINT_FLAGS);
        density = resources.getDisplayMetrics().densityDpi;
        setBitmap(bitmap);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    int width;
    int height;
    public IonDrawable setBitmap(Bitmap bitmap) {
        if (bitmap == this.bitmap)
            return this;

        this.bitmap = bitmap;
        if (bitmap == null) {
            width = -1;
            height = -1;
        }
        else {
            width = bitmap.getScaledWidth(density);
            height = bitmap.getScaledWidth(density);
        }
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
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    Rect bounds = new Rect();
    @Override
    public void draw(Canvas canvas) {
        if (bitmap == null)
            return;
        copyBounds(bounds);
        canvas.drawBitmap(bitmap, null, bounds, paint);
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