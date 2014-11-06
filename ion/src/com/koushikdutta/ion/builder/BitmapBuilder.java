package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.bitmap.PostProcess;
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
     * Resize the bitmap to the given width dimension, maintaining
     * the aspect ratio of the height.
     * @param width
     * @return
     */
    public B resizeWidth(int width);

    /**
     * Resize the bitmap to the given height dimension, maintaining
     * the aspect ratio of the width.
     * @param height
     * @return
     */
    public B resizeHeight(int height);

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
    public B fitCenter();

    /**
     * Center the image inside of the bounds specified by the ImageView or resize
     * operation.
     * @return
     */
    public B centerInside();

    /**
     * Fit the image inside the bounds specified by the ImageView or the resize
     * operation. This will scale the image so that both dimensions are as large as the
     * requested bounds.
     * @return
     */
    public B fitXY();

    /**
     * Enable/disable automatic resizing to the dimensions of the device when loading the image.
     * @param smartSize
     * @return
     */
    public B smartSize(boolean smartSize);

    /**
     * Process the bitmap on a background thread.
     * @param postProcess
     * @return
     */
    public B postProcess(PostProcess postProcess);
}
