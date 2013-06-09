package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.bitmap.Transform;

/**
* Created by koush on 5/30/13.
*/
public interface IonMutableBitmapRequestPostLoadBuilder extends IonMutableBitmapRequestBuilder, IonImageViewRequestPostLoadBuilder {
    /** {@inheritDoc} */
    public IonMutableBitmapRequestPostLoadBuilder transform(Transform transform);

    /** {@inheritDoc} */
    public IonMutableBitmapRequestPostLoadBuilder centerCrop();

    /** {@inheritDoc} */
    public IonMutableBitmapRequestPostLoadBuilder centerInside();
}
