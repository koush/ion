package com.koushikdutta.ion.builder;

/**
 * Created by koush on 6/10/13.
 */
public interface Builders {

    public interface ImageView {
        public interface F extends ImageViewBuilder<F>, BitmapBuilder<F>, LoadImageViewFutureBuilder {
        }

        public interface M extends MultipartBodyBuilder<M>, F {
        }

        public interface U extends UrlEncodedBuilder<U>, F {
        }

        public interface B extends RequestBuilder<F, B, M, U>, F {
        }
    }

    public interface Any {
        public interface F extends FutureBuilder, ImageViewFutureBuilder {
        }

        public interface M extends MultipartBodyBuilder<M>, F {
        }

        public interface U extends UrlEncodedBuilder<U>, F {
        }

        public interface B extends RequestBuilder<F, B, M, U>, F {
        }
    }
}
