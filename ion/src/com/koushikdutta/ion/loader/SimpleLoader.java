package com.koushikdutta.ion.loader;

import android.content.Context;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.future.ResponseFuture;

import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Created by koush on 12/22/13.
 */
public class SimpleLoader implements Loader {
    @Override
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request, FutureCallback<LoaderEmitter> callback) {
        return null;
    }

    @Override
    public Future<BitmapInfo> loadBitmap(Context context, Ion ion, String key, String uri, int resizeWidth, int resizeHeight, boolean animateGif) {
        return null;
    }

    @Override
    public Future<AsyncHttpRequest> resolve(Context context, Ion ion, AsyncHttpRequest request) {
        return null;
    }

    @Override
    public <T> ResponseFuture<T> load(Ion ion, AsyncHttpRequest request, Type type) {
        return null;
    }
}
