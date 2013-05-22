package com.koushikdutta.ion.loader;

import com.koushikdutta.async.future.FutureDataEmitter;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

/**
 * Created by koush on 5/22/13.
 */
public class HttpLoader implements Loader {
    @Override
    public FutureDataEmitter load(Ion ion, AsyncHttpRequest request) {
        if (!request.getUri().getScheme().startsWith("http"))
            return null;
        return ion.getHttpClient().execute(request);
    }
}
