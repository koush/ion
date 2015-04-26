package com.koushikdutta.ion.builder;

import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

import com.koushikdutta.ion.BitmapDrawableFactory;

/**
* Created by koush on 5/30/13.
*/
public interface ImageViewBuilder<I extends ImageViewBuilder<?>> {
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

    /**
     * Configure the fadeIn when the image loads.
     * @return
     */
    public I fadeIn(boolean fadeIn);

    /**
     * Flag to enable or disable animation of GIFs
     * @param mode
     * @return
     */
    public I animateGif(AnimateGifMode mode);

    /**
     * Load the ImageView with a deep zoomable image. This allows extremely large images
     * to be loaded, at full fidelity. Only portions of the image will be decoded,
     * on an as needed basis when rendering.
     * This only works on API level 10+, where BitmapRegionDecoder is available.
     * @return
     */
    public I deepZoom();

    /**
     * Crossfade the new image with the existing image.
     * @return
     */
    public I crossfade(boolean crossfade);

    /**
     * Provide an Drawable factory to control how the loaded Bitmap is drawn.
     * Handy for RoundedDrawables, etc.
     * @param bitmapDrawableFactory
     * @return
     */
    public I bitmapDrawableFactory(BitmapDrawableFactory bitmapDrawableFactory);
}
