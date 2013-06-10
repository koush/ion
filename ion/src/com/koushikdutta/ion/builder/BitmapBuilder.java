package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.bitmap.Transform;

/**
* Created by koush on 5/30/13.
*/
public interface BitmapBuilder<B extends BitmapBuilder> {
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
}
