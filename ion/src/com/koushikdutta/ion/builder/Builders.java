package com.koushikdutta.ion.builder;

import android.graphics.Bitmap;

import com.koushikdutta.ion.bitmap.BitmapInfo;

/**
 * Created by koush on 6/10/13.
 */
public interface Builders {

    public interface IV {
        public interface F<A extends F<?>> extends ImageViewBuilder<A>, BitmapBuilder<A>, LoadImageViewFutureBuilder {
            BitmapInfo getBitmapInfo();
            Bitmap getBitmap();
        }
    }

    public interface Any {
        // restrict to image view builder
        public interface IF<A extends IF<?>> extends ImageViewBuilder<A>, ImageViewFutureBuilder {
        }

        // restrict to bitmap future builder
        public interface BF<A extends BF<?>> extends BitmapBuilder<A>, BitmapFutureBuilder, IF<A> {
        }

        // restrict to future builder
        public interface F extends FutureBuilder, ImageViewFutureBuilder {
        }

        // restrict to multipart builder
        public interface M extends MultipartBodyBuilder<M>, F {
        }

        // restrict to url encoded builder builder
        public interface U extends UrlEncodedBuilder<U>, F {
        }

        // top level builder
        public interface B extends RequestBuilder<F, B, M, U>, F {
        }
    }
}
