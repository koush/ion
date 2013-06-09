package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.bitmap.Transform;

/**
* Created by koush on 5/30/13.
*/
public interface IonMutableBitmapRequestPreLoadBuilder extends IonMutableBitmapRequestBuilder, IonBitmapFutureRequestBuilder, IonImageViewRequestBuilder {
    /** {@inheritDoc} */
    public IonMutableBitmapRequestPreLoadBuilder transform(Transform transform);

    /** {@inheritDoc} */
    public IonMutableBitmapRequestPreLoadBuilder centerCrop();

    /** {@inheritDoc} */
    public IonMutableBitmapRequestPreLoadBuilder centerInside();
}
