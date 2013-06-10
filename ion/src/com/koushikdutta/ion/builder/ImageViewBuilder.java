package com.koushikdutta.ion.builder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

/**
* Created by koush on 5/30/13.
*/
public interface ImageViewBuilder<I extends ImageViewBuilder & BitmapBuilder> extends BitmapBuilder<I> {
    /**
     * Set a placeholder on the ImageView while the request is loading
     * @param bitmap
     * @return
     */
    public I placeholder(Bitmap bitmap);

    /**
     * Set a placeholder on the ImageView while the request is loading
     * @param drawable
     * @return
     */
    public I placeholder(Drawable drawable);

    /**
     * Set a placeholder on the ImageView while the request is loading
     * @param resourceId
     * @return
     */
    public I placeholder(int resourceId);

    /**
     * Set an error image on the ImageView if the request fails to load
     * @param bitmap
     * @return
     */
    public I error(Bitmap bitmap);

    /**
     * Set an error image on the ImageView if the request fails to load
     * @param drawable
     * @return
     */
    public I error(Drawable drawable);

    /**
     * Set an error image on the ImageView if the request fails to load
     * @param resourceId
     * @return
     */
    public I error(int resourceId);

    /**
     * If an ImageView is loaded successfully from a remote source or file storage,
     * animate it in using the given Animation. The default animation is to fade
     * in.
     * @param in Animation to apply to the ImageView after the request has loaded
     *           and the Bitmap has been retrieved.
     * @return
     */
    public I animateIn(Animation in);

    /**
     * If an ImageView is loaded successfully from a remote source or file storage,
     * animate it in using the given Animation resource. The default animation is to fade
     * in.
     * @param animationResource Animation resource to apply to the ImageView after the request has loaded
     *           and the Bitmap has been retrieved.
     * @return
     */
    public I animateIn(int animationResource);

    /**
     * If the ImageView needs to load from a remote source or file storage,
     * the given Animation will be used while it is loading.
     * @param load Animation to apply to the imageView while the request is loading.
     * @return
     */
    public I animateLoad(Animation load);

    /**
     * If the ImageView needs to load from a remote source or file storage,
     * the given Animation resource will be used while it is loading.
     * @param animationResource Animation resource to apply to the imageView while the request is loading.
     * @return
     */
    public I animateLoad(int animationResource);
}
