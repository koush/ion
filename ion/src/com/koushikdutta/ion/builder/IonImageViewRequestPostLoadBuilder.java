package com.koushikdutta.ion.builder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.koushikdutta.async.future.Future;

/**
* Created by koush on 5/30/13.
*/
public interface IonImageViewRequestPostLoadBuilder extends IonImageViewRequestBuilder {
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder placeholder(Bitmap bitmap);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder placeholder(Drawable drawable);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder placeholder(int resourceId);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder error(Bitmap bitmap);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder error(Drawable drawable);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder error(int resourceId);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder animateIn(Animation in);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder animateIn(int animationResource);
    /** {@inheritDoc} */
    public IonImageViewRequestPostLoadBuilder animateLoad(Animation load);
    /** {@inheritDoc} */
    public IonImageViewRequestBuilder animateLoad(int animationResource);

    /**
     * Load a uri for the ImageView.
     * @param uri Uri to load. This may be a http(s), file, or content uri.
     * @return
     */
    public Future<ImageView> load(String uri);

    /**
     * Load a uri for the ImageView using the given an HTTP method such as GET or POST.
     * @param method HTTP method such as GET or POST.
     * @param uri Uri to load.
     * @return
     */
    public Future<ImageView> load(String method, String uri);
}
