package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.bitmap.Transform;

/**
* Created by koush on 5/30/13.
*/
public interface IonMutableBitmapRequestBuilder {
    /**
     * Apply a transformation to a Bitmap
     * @param transform Transform to apply
     * @return
     */
    public IonMutableBitmapRequestBuilder transform(Transform transform);

    /**
     * Center the image inside of the bounds specified by the ImageView or resize
     * operation. This will scale the image so that it fills the bounds, and crops
     * the extra.
     * @return
     */
    public IonMutableBitmapRequestBuilder centerCrop();

    /**
     * Center the image inside of the bounds specified by the ImageView or resize
     * operation. This will scale the image so that one dimension is as large as the requested
     * bounds.
     * @return
     */
    public IonMutableBitmapRequestBuilder centerInside();
}
