# Change Log

## Version 2.0.0

_2014-11-05_

* Add support for SPDY. (HTTP/2 exists, is not enabled)
* Conscrypt is the new SSL provider on all devices supporting GMS. Better https stack, with no changes necessary on your part.
* Much lower memory footprint for ImageView operations. Faster too:
 * ImageView network requests are initiated onDraw (whereas they were immediately executed before). This results in automatic prioritization of on screen vs off screen ImageViews.
 * ImageView requests can now be center(Crop/Inside/etc) without a .resize(x,y) call. The ImageView dimensions are retrieved at draw time, and an intelligent decode sample size is used to fit the target dimensions. Eliminating the resize call also halves the total time it takes to get the image on screen. This is recommended.
 * GIF decoding is progressive. That means it is not loading all the frames up front before displaying the GIF. Tradeof is much less memory is used in exchange for more CPU time while the GIF is on screen.
