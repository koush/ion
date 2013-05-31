package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.bitmap.Transform;

/**
* Created by koush on 5/30/13.
*/
public interface IonMutableBitmapRequestBuilder extends IonBitmapFutureRequestBuilder, IonImageViewRequestBuilder {
    /**
     * Apply a transformation to a Bitmap
     * @param transform Transform to apply
     * @return
     */
    public IonMutableBitmapRequestBuilder transform(Transform transform);
}
