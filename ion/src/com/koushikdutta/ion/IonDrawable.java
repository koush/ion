package com.koushikdutta.ion;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Created by koush on 6/8/13.
 */
public class IonDrawable extends Drawable {
    private Paint paint;
    private Bitmap bitmap;
    private int density;
    private IonBitmapRequestBuilder.ScaleMode scaleMode;

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
            height = bitmap.getScaledHeight(density);
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

    private void recompute() {

    }

    public IonDrawable setScaleMode(IonBitmapRequestBuilder.ScaleMode scaleMode) {
        if (this.scaleMode == scaleMode)
            return this;
        this.scaleMode = scaleMode;

        if (bitmap != null) {
            width = bitmap.getScaledWidth(density);
            height = bitmap.getScaledHeight(density);
        }

        invalidateSelf();
        return this;
    }

    public IonDrawable setDensity(int density) {
        if (this.density == density)
            return this;
        this.density = density;
        invalidateSelf();
        return this;
    }

    public IonDrawable setIntrinsicDimensions(int width, int height) {
        assert scaleMode == IonBitmapRequestBuilder.ScaleMode.CenterCrop;
        if (this.width == width && this.height == height)
            return this;

        if (width == 0 && height == 0) {
            // wtf?
        }
        else if (width == 0) {
            float ratio = (float)this.width / (float)this.height;
            this.width = (int)(ratio * height);
            this.height = height;
        }
        else if (height == 0) {
            float ratio = (float)this.height / (float)this.width;
            this.width = width;
            this.height = (int)(ratio * width);
        }
        else {
            this.width = width;
            this.height = height;
        }

        invalidateSelf();
        return this;
    }

    Rect drawBounds = new Rect();
    @Override
    public void draw(Canvas canvas) {
        if (bitmap == null)
            return;

        Rect bounds = getBounds();

        float xratio = (float)bounds.width() / (float)bitmap.getWidth();
        float yratio = (float)bounds.height() / (float)bitmap.getHeight();

        float ratio;
        if (scaleMode == IonBitmapRequestBuilder.ScaleMode.CenterCrop)
            ratio = Math.max(xratio, yratio);
        else
            ratio = Math.min(xratio, yratio);


        int newWidth = (int)(ratio * bitmap.getWidth());
        int newHeight = (int)(ratio * bitmap.getHeight());
        int startx = (bounds.width() - newWidth) >> 1;
        int starty = (bounds.height() - newHeight) >> 1;

        drawBounds.set(startx, starty, startx + newWidth, starty + newHeight);

        canvas.drawBitmap(bitmap, null, drawBounds, paint);
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