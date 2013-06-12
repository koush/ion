package com.koushikdutta.ion;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import com.koushikdutta.ion.bitmap.BitmapInfo;

/**
 * Created by koush on 6/8/13.
 */
public class IonDrawable extends Drawable {
    private Paint paint;
    private Bitmap bitmap;
    private BitmapInfo info;
    private Drawable placeholder;
    private Drawable error;

    private static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

    public IonDrawable() {
        paint = new Paint(DEFAULT_PAINT_FLAGS);
    }

    int width;
    int height;
    public IonDrawable setBitmap(BitmapInfo info, int width, int height) {
        Bitmap bitmap = info.bitmap;
        this.info = info;
        if (bitmap == this.bitmap && this.width == width && this.height == height)
            return this;

        this.bitmap = bitmap;
        this.width = width;
        this.height = height;
        invalidateSelf();
        return this;
    }

    public IonDrawable setError(Drawable drawable, int width, int height) {
        if (drawable == this.error && this.width == width && this.height == height)
            return this;

        this.width = width;
        this.height = height;
        this.error = drawable;
        invalidateSelf();
        return this;
    }

    public IonDrawable setPlaceholder(Drawable drawable, int width, int height) {
        if (drawable == this.placeholder && this.width == width && this.height == height)
            return this;

        this.width = width;
        this.height = height;
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
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap == null) {
            if (placeholder != null) {
                placeholder.setBounds(getBounds());
                placeholder.draw(canvas);
            }
            return;
        }

        canvas.drawBitmap(bitmap, null, getBounds(), paint);

        if (true)
            return;

        // stolen from picasso
        canvas.save();
        canvas.rotate(45);

        paint.setColor(Color.WHITE);
        canvas.drawRect(0, -10, 7.5f, 10, paint);

        int sourceColor;
        switch (info.loadedFrom) {
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