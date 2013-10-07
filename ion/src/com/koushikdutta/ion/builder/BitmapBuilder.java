package com.koushikdutta.ion.builder;

import android.graphics.Rect;
import android.graphics.RectF;

import com.koushikdutta.ion.bitmap.Transform;

/**
* Created by koush on 5/30/13.
*/
public interface BitmapBuilder<B extends BitmapBuilder<?>> {
    /**
     * Apply a transformation to a Bitmap
     * @param transform Transform to apply
     * @return
     */
    public B transform(Transform transform);

    /**
     * Resize the bitmap to the given dimensions.
     * @param width
     * @param height
     * @return
     */
    public B resize(int width, int height);

    /**
     * Center the image inside of the bounds specified by the ImageView or resize
     * operation. This will scale the image so that it fills the bounds, and crops
     * the extra.
     * @return
     */
    public B centerCrop();

    /**
     * Center the image inside of the bounds specified by the ImageView or resize
     * operation. This will scale the image so that one dimension is as large as the requested
     * bounds.
     * @return
     */
    public B centerInside();

    /**
     * Load only the requested region of the image as the initial bitmap.
     * @param sourceRect
     * @return
     */
    public B region(RectF sourceRect);
}
