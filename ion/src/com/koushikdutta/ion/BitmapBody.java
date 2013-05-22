package com.koushikdutta.ion;

import android.graphics.Bitmap;
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import com.koushikdutta.async.http.server.AsyncHttpRequestBodyBase;

/**
 * Created by koush on 5/22/13.
 */
class BitmapBody extends AsyncHttpRequestBodyBase<Bitmap> {
    Bitmap bitmap;

    public BitmapBody() {
        super("image/bitmap");
    }

    @Override
    public Bitmap get() {
        return bitmap;
    }
}
