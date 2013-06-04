package com.koushikdutta.ion.builder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

/**
* Created by koush on 5/30/13.
*/
public interface IonImageViewRequestPreLoadBuilder extends IonImageViewRequestBuilder, IonBitmapImageViewFutureRequestBuilder {
    /** {@inheritDoc} */
    public IonImageViewRequestPreLoadBuilder placeholder(Bitmap bitmap);
    /** {@inheritDoc} */
    public IonImageViewRequestPreLoadBuilder placeholder(Drawable drawable);
    /** {@inheritDoc} */
    public IonImageViewRequestPreLoadBuilder placeholder(int resourceId);
    /** {@inheritDoc} */
    public IonImageViewRequestPreLoadBuilder error(Bitmap bitmap);
    /** {@inheritDoc} */
    public IonImageViewRequestPreLoadBuilder error(Drawable drawable);
    /** {@inheritDoc} */
    public IonImageViewRequestPreLoadBuilder error(int resourceId);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder animateIn(Animation in);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder animateIn(int animationResource);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder animateLoad(Animation load);
    /** {@inheritDoc} */
    public IonImageViewRequestBuilder animateLoad(int animationResource);
}
