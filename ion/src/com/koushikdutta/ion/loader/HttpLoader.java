package com.koushikdutta.ion.loader;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.callback.HttpConnectCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

/**
 * Created by koush on 5/22/13.
 */
public class HttpLoader implements Loader {
    @SuppressWarnings("unchecked")
    @Override
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request, final FutureCallback<DataEmitter> callback) {
        if (!request.getUri().getScheme().startsWith("http"))
            return null;
        return (Future< DataEmitter >)(Future)ion.getHttpClient().execute(request, new HttpConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, AsyncHttpResponse response) {
                callback.onCompleted(ex, response);
            }
        });
    }
}
